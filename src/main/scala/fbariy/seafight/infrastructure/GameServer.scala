package fbariy.seafight.infrastructure

import cats.effect.{
  Blocker,
  ConcurrentEffect,
  ContextShift,
  Resource,
  Sync,
  Timer
}
import cats.implicits._
import fbariy.seafight.application.game.CanMakeMoveHandler
import fbariy.seafight.application.invite.{
  CreateInviteHandler,
  CreateInviteValidator
}
import fbariy.seafight.application.ship.{AddShipsHandler, AddShipsValidator}
import fbariy.seafight.infrastructure.config.{AppConfig, DBConfig}
import fbariy.seafight.infrastructure.endpoint._
import fbariy.seafight.infrastructure.repository._
import org.http4s.implicits._
import org.http4s.server.blaze.BlazeServerBuilder
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
      implicit T: Timer[F]): Resource[F, Server[F]] = {
    val blocker = Blocker.liftExecutionContext(global)

    for {
      cfg        <- Resource.eval(config(blocker))
      transactor <- DBConfig.transactor(cfg.db, global, blocker)

      preparationHdlr = new AddShipsHandler[F](
        new InMemoryShipsRepo[F],
        new DoobieGameRepo[F](transactor),
        new AddShipsValidator
      )
      createInviteHdlr = new CreateInviteHandler[F](
        new CreateInviteValidator,
        new DoobieInviteRepo[F](transactor)
      )
      canMoveHdlr = new CanMakeMoveHandler
      gameRepo    = new DoobieGameRepo[F](transactor)
      inviteRepo  = new DoobieInviteRepo[F](transactor)

      preparationEndpoints = new PreparationEndpoints[F]
      gameEndpoints        = new GameEndpoints[F]

      httpApp = Router[F](
        "api/v1/preparation" -> (
          preparationEndpoints.example(transactor) <+>
            preparationEndpoints.createInvite(createInviteHdlr) <+>
            withInvite(inviteRepo)(
              preparationEndpoints.addShips(preparationHdlr))
        ),
        "api/v1/game" -> withGame(gameRepo)(
          gameEndpoints.canMakeMove(canMoveHdlr))
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
