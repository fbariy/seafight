package fbariy.seafight.infrastructure

import cats.effect.concurrent.Semaphore
import cats.effect.{
  Blocker,
  ConcurrentEffect,
  ContextShift,
  Resource,
  Sync,
  Timer
}
import cats.implicits._
import fbariy.seafight.application.game.{
  CanMakeMoveHandler,
  MoveHandler,
  MoveValidator
}
import fbariy.seafight.application.invite.{
  CreateInviteHandler,
  CreateInviteValidator
}
import fbariy.seafight.application.ship.{AddShipsHandler, AddShipsValidator}
import fbariy.seafight.infrastructure.config.{AppConfig, DBConfig}
import fbariy.seafight.infrastructure.endpoint._
import fbariy.seafight.infrastructure.repository._
import org.http4s.implicits._
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.server.{Router, Server}
import pureconfig.ConfigSource
import pureconfig.module.catseffect2.syntax._

import scala.concurrent.ExecutionContext.global

object GameServer {
  def config[F[_]: Sync: ContextShift](blocker: Blocker): F[AppConfig] = {
    for {
      env <- Sync[F].delay(
        sys.env.getOrElse("SEAFIGHT_ENV", "dev")
      )
      conf <- ConfigSource
        .resources(s"application.$env.conf")
        .recoverWith(_ => ConfigSource.default)
        .loadF[F, AppConfig](blocker)
    } yield conf
  }

  def resource[F[_]: ConcurrentEffect: ContextShift](
      implicit T: Timer[F]): Resource[F, Server] = {
    val blocker = Blocker.liftExecutionContext(global)

    for {
      cfg        <- Resource.eval(config(blocker))
      transactor <- DBConfig.transactor(cfg.db, global, blocker)

      gameRepo = new DoobieGameRepository[F](transactor)

      addShipsSemaphore <- Resource.eval(Semaphore[F](1))
      preparationHdlr = new AddShipsHandler[F](
        new InMemoryShipsRepository[F],
        new DoobieGameRepository[F](transactor),
        new AddShipsValidator[F](gameRepo),
        addShipsSemaphore
      )
      createInviteHdlr = new CreateInviteHandler[F](
        new CreateInviteValidator,
        new DoobieInviteRepository[F](transactor)
      )
      moveValidator = new MoveValidator
      canMoveHdlr   = new CanMakeMoveHandler[F](moveValidator)
      inviteRepo    = new DoobieInviteRepository[F](transactor)

      moveHandlerSemaphore <- Resource.eval(Semaphore[F](1))
      moveHdlr = new MoveHandler(gameRepo, moveValidator, moveHandlerSemaphore)

      preparationEndpoints = new PreparationEndpoints[F]
      gameEndpoints        = new GameEndpoints[F]

      httpApp = Router[F](
        "api/v1/preparation" -> (
          preparationEndpoints.example(transactor) <+>
            preparationEndpoints.createInvite(createInviteHdlr) <+>
            withInvite(inviteRepo)(
              preparationEndpoints.addShips(preparationHdlr))
        ),
        "api/v1/game" -> (withGame(gameRepo)(
          gameEndpoints.canMakeMove(canMoveHdlr)) <+>
          withGame(gameRepo)(gameEndpoints.move(moveHdlr)))
      )

      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(
        httpApp.orNotFound)

      exitCode <- BlazeServerBuilder[F](global)
        .bindHttp(8000, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .resource
    } yield exitCode
  }
}
