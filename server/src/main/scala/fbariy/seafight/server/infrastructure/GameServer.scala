package fbariy.seafight.server.infrastructure

import cats.effect.std.Semaphore
import cats.effect.{Async, Resource, Sync}
import fbariy.seafight.server.application.back.{
  AcceptBackHandler,
  BackToMoveHandler,
  BackToMoveValidator,
  CancelBackHandler
}
import fbariy.seafight.server.application.game.{
  CanMakeMoveHandler,
  MoveHandler,
  MoveValidator
}
import fbariy.seafight.server.application.invite.{
  CreateInviteHandler,
  CreateInviteValidator
}
import fbariy.seafight.server.application.notification.ReleaseNotificationsHandler
import fbariy.seafight.server.application.ship.{
  AddShipsHandler,
  AddShipsValidator
}
import fbariy.seafight.server.infrastructure.config.{AppConfig, DBConfig}
import fbariy.seafight.server.infrastructure.endpoint._
import fbariy.seafight.server.infrastructure.notification.InMemoryNotificationBus
import fbariy.seafight.server.infrastructure.repository.{
  DoobieGameRepository,
  DoobieInviteRepository,
  InMemoryBackToMoveRepository,
  InMemoryShipsRepository
}
import org.http4s.blaze.server.BlazeServerBuilder
import org.http4s.server.middleware.Logger
import org.http4s.server.{Router, Server}
import pureconfig.ConfigSource
import cats.implicits._
import fbariy.seafight.server.application.canplay.CanPlayHandler
import pureconfig.module.catseffect.syntax._

import scala.concurrent.ExecutionContext.global

object GameServer {
  def config[F[_]: Sync]: F[AppConfig] = {
    for {
      env <- Sync[F].delay(
        sys.env.getOrElse("SEAFIGHT_ENV", "dev")
      )
      conf <- ConfigSource
        .resources(s"application.$env.conf")
        .recoverWith(_ => ConfigSource.default)
        .loadF[F, AppConfig]()
    } yield conf
  }

  def resource[F[_]: Async]: Resource[F, Server] = {
    for {
      cfg        <- Resource.eval(config[F])
      transactor <- DBConfig.transactor(cfg.db, global)

      bus       = new InMemoryNotificationBus[F]
      gameRepo  = new DoobieGameRepository[F](transactor)
      shipsRepo = new InMemoryShipsRepository[F]

      addShipsSemaphore <- Resource.eval(Semaphore[F](1))
      preparationHdlr = new AddShipsHandler[F](
        shipsRepo,
        gameRepo,
        new AddShipsValidator[F](gameRepo),
        bus,
        addShipsSemaphore
      )
      createInviteHdlr = new CreateInviteHandler[F](
        new CreateInviteValidator,
        new DoobieInviteRepository[F](transactor)
      )
      moveValidator  = new MoveValidator
      backRepository = new InMemoryBackToMoveRepository[F]
      backValidator  = new BackToMoveValidator[F](backRepository)
      canMoveHdlr    = new CanMakeMoveHandler[F](moveValidator)
      inviteRepo     = new DoobieInviteRepository[F](transactor)

      moveHandlerSemaphore <- Resource.eval(Semaphore[F](1))
      moveHdlr = new MoveHandler(gameRepo,
                                 moveValidator,
                                 backValidator,
                                 bus,
                                 moveHandlerSemaphore)

      backToMoveSemaphore <- Resource.eval(Semaphore[F](1))
      backToMoveHdlr = new BackToMoveHandler[F](backValidator,
                                                backRepository,
                                                bus,
                                                backToMoveSemaphore)

      cancelBackSemaphore <- Resource.eval(Semaphore[F](1))
      cancelBackHdlr = new CancelBackHandler[F](backValidator,
                                                backRepository,
                                                bus,
                                                cancelBackSemaphore)

      acceptBackSemaphore <- Resource.eval(Semaphore[F](1))
      acceptBackHdlr = new AcceptBackHandler[F](backValidator,
                                                backRepository,
                                                gameRepo,
                                                bus,
                                                acceptBackSemaphore)

      releaseNotificationsHdlr = new ReleaseNotificationsHandler[F](bus)
      canPlayHdlr              = new CanPlayHandler[F](gameRepo, shipsRepo)

      preparationEndpoints   = new PreparationEndpoints[F]
      gameEndpoints          = new GameEndpoints[F]
      notificationsEndpoints = new NotificationEndpoints[F]

      httpApp = Router[F](
        "api/v1/preparation" -> (
          preparationEndpoints.createInvite(createInviteHdlr) <+>
            withInvite(inviteRepo)(
              preparationEndpoints.addShips(preparationHdlr)) <+>
            withInvite(inviteRepo)(preparationEndpoints.getInvite) <+>
            withInvite(inviteRepo)(preparationEndpoints.canPlay(canPlayHdlr))
        ),
        "api/v1/game" -> (
          withGame(gameRepo)(gameEndpoints.canMakeMove(canMoveHdlr)) <+>
            withGame(gameRepo)(gameEndpoints.getGame) <+>
            withGame(gameRepo)(gameEndpoints.move(moveHdlr)) <+>
            withGame(gameRepo)(gameEndpoints.backToMove(backToMoveHdlr)) <+>
            withGame(gameRepo)(gameEndpoints.cancelBack(cancelBackHdlr)) <+>
            withGame(gameRepo)(gameEndpoints.acceptBack(acceptBackHdlr))
        ),
        "api/v1/notification" ->
          withInvite(inviteRepo)(
            notificationsEndpoints.release(releaseNotificationsHdlr))
      )

      finalHttpApp = Logger.httpApp(logHeaders = true, logBody = true)(
        httpApp.orNotFound)

      exitCode <- BlazeServerBuilder[F]
        .bindHttp(8000, "0.0.0.0")
        .withHttpApp(finalHttpApp)
        .resource
    } yield exitCode
  }
}
