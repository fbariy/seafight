package fbariy.seafight
package client

import cats.data.NonEmptyChain
import cats.effect.implicits._
import cats.effect.std.Console
import cats.effect.{Async, Resource, Sync, Temporal}
import cats.implicits._
import cats.{ApplicativeError, FlatMap, Show}
import fbariy.seafight.client.infrastructure.configuration.AppConfig
import fbariy.seafight.core.application.error._
import fbariy.seafight.core.application.error.instances._
import fbariy.seafight.core.application.notification.AppNotification
import fbariy.seafight.core.application.{
  AppErrorOutput,
  CreateInviteInput,
  GameOutput,
  InviteOutput
}
import fbariy.seafight.core.domain.Symbol._
import fbariy.seafight.core.domain.{
  Cell,
  Digit,
  Player,
  PlayerWithInvite,
  Symbol
}
import fbariy.seafight.core.infrastructure.SeafightClient
import org.http4s.Uri
import org.http4s.blaze.client.BlazeClientBuilder
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._

import java.util.UUID
import scala.concurrent.duration.DurationInt
import scala.util.{Failure, Success, Try}

class GameClient[
    F[_]: Async: FlatMap: Temporal: ApplicativeError[*[_], Throwable]](
    implicit C: Console[F]) {
  import C._

  def config: F[AppConfig] = {
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

  def resource: Resource[F, Unit] =
    for {
      cfg    <- Resource.eval(config)
      client <- BlazeClientBuilder[F].resource

      _ <- Resource.eval(println(s"Хост: ${cfg.host}"))

      //todo: заменить на config_reader
      baseUri = Uri.fromString(cfg.host) match {
        case Left(parseFailure) => throw parseFailure
        case Right(uri)         => uri
      }

      appClient = new SeafightClient[F](client, baseUri)

      _ <- Resource.eval(mainMenu(appClient))
    } yield ()

  private def clear: F[Unit] = print("\u001b[2J") >> print("\u001b[H")

  private def askAccept: F[Unit] =
    print("Нажмите enter для продолжения...") >> readLine.as(())

  def mainMenu(client: SeafightClient[F]): F[Unit] =
    for {
      _      <- clear
      _      <- println("[1] создать новую игру")
      _      <- println("[2] присоединиться к игре")
      _      <- println("[3] выйти")
      action <- print("Выберите действие: ") >> readLine
      _      <- clear
      _ <- action match {
        case "1" =>
          for {
            inviteCtx <- createInviteLoop(client)
            _ <- joinToGameLoop(client)(
              Credentials(inviteCtx.invite.id, inviteCtx.p).some)
          } yield ()
        case "2" => joinToGameLoop(client)(None)
        case "3" => print("Выбрано действие 3")
        case _ =>
          print("Ошибка: Выберите действие из возможных") >> askAccept >> mainMenu(
            client)
      }
    } yield ()

  def createInviteLoop(client: SeafightClient[F]): F[PlayerWithInvite] =
    for {
      _     <- print("Первый игрок: ")
      rawP1 <- readLine
      _     <- print("Второй игрок: ")
      rawP2 <- readLine

      input = CreateInviteInput(Player(rawP1), Player(rawP2))
      result <- client.createInvite(input)
      inviteCtx <- result.fold(
        printErrors(_) >> askAccept >> clear >> createInviteLoop(client),
        invite =>
          for {
            _ <- println(s"ID игры ${invite.id}")
            _ <- println(
              s"Игроки ${invite.player1.name} и ${invite.player2.name}")
            inviteCtx <- askCurrentPlayer(invite)
            _         <- askAccept
            _         <- clear
          } yield inviteCtx
      )
    } yield inviteCtx

  private def askCurrentPlayer(invite: InviteOutput): F[PlayerWithInvite] =
    for {
      _                     <- print("Вы первый игрок? [Y/n]: ")
      currentPlayerIsSecond <- readLine.map(_ == "n")
    } yield invite.toPlayerContext(!currentPlayerIsSecond)

  //todo: выводить развернутую ошибку
  def printErrors(errors: NonEmptyChain[AppError]): F[Unit] =
    println("Ошибки: ") >> println(errors.show)

  implicit val errorsShow: Show[NonEmptyChain[AppErrorOutput]] =
    (errors: NonEmptyChain[AppErrorOutput]) => errors.mkString_("\n")

  def readCell: F[Either[String, Cell]] =
    for {
      rawCell <- print("Клетка (примеры: A:1, I:9): ") >> readLine
    } yield parseCell(rawCell.trim)

  def parseCell(cell: String): Either[String, Cell] =
    for {
      _ <- Either.cond(cell.length == 3,
                       (),
                       s"Формат клетки 3 символа, передано: ${cell.length}")
      symbol <- Symbol
        .withNameInsensitiveOption(cell(0).toString)
        .toRight("Формат символа клетки A-Z")
      _ <- Either.cond(cell(1) == ':',
                       (),
                       s"Разделитель всегда ':', передан: '${cell(1)}'")
      digit <- Digit
        .withNameOption(cell(2).toString)
        .toRight("Формат номера клетки 1-9")
    } yield symbol \ digit

  def checkUniqueShips(ships: Set[Cell])(cell: Cell): Either[String, Cell] =
    Either.cond(!ships.contains(cell), cell, "Клетка уже выбрана")

  def setupShipsLoop(client: SeafightClient[F])(inviteId: UUID,
                                                p: Player): F[Unit] = {
    def manualSetupLoop(ships: Set[Cell]): F[Set[Cell]] =
      for {
        _          <- println(viewShips(ships))
        _          <- println(s"Ввод ${ships.size + 1}/20 клетки")
        cellResult <- readCell
        ships <- (cellResult >>= (checkUniqueShips(ships)(_)))
          .fold(
            print("Ошибка: ") >> println(_) >> askAccept >> clear >> manualSetupLoop(
              ships),
            cell => {
              val updatedShips = ships + cell
              clear >> (if (updatedShips.size == 20) updatedShips.pure[F]
                        else manualSetupLoop(updatedShips))
            }
          )
      } yield ships

    def fillShipsLoop: F[Set[Cell]] =
      for {
        _ <- println("[1] расставить корабли случайно")
        _ <- println("[2] расставить корабли вручную")

        action <- print("Выберите действие: ") >> readLine
        _      <- clear

        ships <- action match {
          case "1" => Set.empty[Cell].pure[F] //todo: coming soon
          case "2" => manualSetupLoop(Set.empty[Cell])
          case _   => println("Выберите действие из возможных") >> fillShipsLoop
        }
      } yield ships

    for {
      ships  <- fillShipsLoop
      result <- client.addShips(inviteId, p)(ships.toSeq)
      _ <- result.fold(
        printErrors(_) >> setupShipsLoop(client)(inviteId, p),
        _ => ().pure[F]
      )
    } yield ()
  }

  //todo: расширить до 1-10, A-J
  //todo: реверснуть порядок номеров
  def viewShips(ships: Set[Cell]): String = {
    val symbols = (1 to 9).map(matchSymbol)

    val matrix: Seq[Seq[Cell]] = (1 to 9).reverse.map(number =>
      symbols.map(symbol => symbol \ Digit.withName(number.toString)))

    val matrixWithShips =
      matrix.map(_.map(cell => if (ships(cell)) "@" else " "))

    val symbolsLine = s"| ${symbols.map(_.entryName).mkString(" | ")} |"
    val lines       = matrixWithShips.map(row => "| " + row.mkString(" | ") + " |")

    (symbolsLine +: lines)
      .zip((1 to 10).reverse)
      .map {
        case line -> index =>
          val indexStr = if (index == 10) "-" else index.toString
          s"| $indexStr |$line"
      }
      .mkString("\n")
  }

  private def matchSymbol: PartialFunction[Int, Symbol] = {
    case 1 => A
    case 2 => B
    case 3 => Symbol.C
    case 4 => D
    case 5 => E
    case 6 => F
    case 7 => G
    case 8 => H
    case 9 => I
    case _ => throw new IllegalArgumentException
  }

  def askCredentials: F[Credentials] =
    for {
      id     <- print("Введите ID игры: ") >> readLine
      player <- print("Введите имя игрока: ") >> readLine
      credentials <- Try(UUID.fromString(id)) match {
        case Failure(_) =>
          println("Ошибка: не валидный UUID") >> askAccept >> clear >> askCredentials
        case Success(uuid) => Credentials(uuid, Player(player)).pure[F]
      }
    } yield credentials

  def joinToGameLoop(client: SeafightClient[F])(
      credentials: Option[Credentials]): F[Unit] =
    for {
      crd @ Credentials(id, p) <- credentials.fold(askCredentials)(_.pure[F])
      canPlay                  <- client.canPlay(id, p)
      _ <- canPlay.fold(
        errors =>
          errors
            .collectFirst {
              case ShipsAreNotSetupError(_, player) if player == p =>
                clear >> setupShipsLoop(client)(id, p) >> joinToGameLoop(
                  client)(crd.some)
              case ShipsAreNotSetupError(_, _) =>
                for {
                  _ <- println("Ожидание расстановки кораблей оппонентом...")
                  _ <- Temporal[F].sleep(3.seconds)
                  _ <- clear >> joinToGameLoop(client)(crd.some)
                } yield ()
            }
            .getOrElse(printErrors(errors) >> askAccept >> mainMenu(client)),
        _ => gameMenuLoop(client)(crd)
      )
    } yield ()

  case class GameState(game: GameOutput,
                       notifications: FixedQueue[AppNotification])

  def refreshState(client: SeafightClient[F])(credentials: Credentials)(
      oldState: Option[GameState] = None): F[GameState] =
    for {
      (gameResult, notificationsResult) <- (
        client.getGame(credentials.gameId, credentials.p),
        client.release(credentials.gameId, credentials.p)
      ).parTupled
      actualGame <- ApplicativeError[F, Throwable]
        .fromOption(gameResult.toOption, new Throwable("Game must be created"))
      notifications = notificationsResult.fold(_ => List.empty,
                                               notifications => notifications)
      updatedState = oldState.fold(
        GameState(actualGame, FixedQueue(5, notifications: _*)))(
        state =>
          GameState(actualGame, state.notifications.enqueueAll(notifications))
      )
    } yield updatedState

  def viewState(state: GameState): String = "Состояние игры" //todo: implements

  def askGameActionAndUpdateStateLoop(client: SeafightClient[F])(
      credentials: Credentials,
      state: Option[GameState] = None): F[(String, GameState)] =
    for {
      refreshed <- refreshState(client)(credentials)(state)
      _         <- println(viewState(refreshed))
      _         <- printGameMenu
      _         <- println("Выберите действие: ")
      result <- readLine
        .map(_ -> refreshed)
        .timeoutTo(
          2.seconds,
          clear >> askGameActionAndUpdateStateLoop(client)(credentials,
                                                           refreshed.some))
    } yield result

  def printGameMenu: F[Unit] = println("""
                                         |[1] сделать ход
                                         |[2] запросить возврат хода
                                         |[3] реплей
                                         |[4] выйти""".stripMargin)

  case class Credentials(gameId: UUID, p: Player)

  def gameMenuLoop(client: SeafightClient[F])(
      credentials: Credentials,
      state: Option[GameState] = None): F[Unit] =
    for {
      (action, actualState) <- askGameActionAndUpdateStateLoop(client)(
        credentials,
        state)
      _ <- action match {
        case "1" =>
          makeMoveLoop(client)(actualState.game)(credentials) >> gameMenuLoop(
            client)(credentials, actualState.some)
        case "4" => ().pure[F]
        case _ =>
          println("Ошибка: Выберите действие из возможных") >> askAccept >> gameMenuLoop(
            client)(credentials, actualState.some)
      }
    } yield ()

  //todo: вывод статуса попадания
  def makeMoveLoop(client: SeafightClient[F])(game: GameOutput)(
      credentials: Credentials): F[Unit] =
    for {
      cellResult <- readCell
      _ <- (cellResult >>= (checkUniqueShips(game.ships.toSet)(_))).fold(
        error =>
          println(s"Ошибка: $error") >> askAccept >> clear >> makeMoveLoop(
            client)(game)(credentials),
        kick =>
          for {
            validated <- client.move(credentials.gameId, credentials.p)(kick)
            _ <- validated.fold(
              errors => printErrors(errors) >> askAccept >> clear,
              _ => ().pure[F]
            )
          } yield ()
      )
    } yield ()
}
