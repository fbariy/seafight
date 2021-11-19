package fbariy.seafight
package client

import cats.data.NonEmptyChain
import cats.effect.kernel.Concurrent
import cats.effect.{Async, Resource, Sync}
import cats.implicits._
import cats.{Applicative, Show}
import fbariy.seafight.client.infrastructure.configuration.AppConfig
import fbariy.seafight.core.application.{AppErrorOutput, CreateInviteInput, InviteOutput}
import fbariy.seafight.core.domain.Symbol._
import fbariy.seafight.core.domain.{Cell, Digit, Player, PlayerWithInvite, Symbol}
import fbariy.seafight.core.infrastructure.SeafightClient
import org.http4s.Uri
import org.http4s.blaze.client.BlazeClientBuilder
import pureconfig.ConfigSource
import pureconfig.module.catseffect.syntax._

import java.util.UUID
import scala.io.StdIn._

object GameClient {
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

  def resource[F[_]: Async: Concurrent]: Resource[F, Unit] =
    for {
      cfg    <- Resource.eval(config)
      client <- BlazeClientBuilder[F].resource

      _ <- Resource.eval(Sync[F].delay(printLine(s"Хост: ${cfg.host}")))

      //todo: заменить на config_reader
      baseUri = Uri.fromString(cfg.host) match {
        case Left(parseFailure) => throw parseFailure
        case Right(uri)         => uri
      }

      appClient = new SeafightClient[F](client, baseUri)

      _ <- Resource.eval(menu(appClient))
    } yield ()

  private def printLine[F[_]: Sync](str: String): F[Unit] =
    Sync[F].delay(println(str))

  private def printStr[F[_]: Sync](str: String): F[Unit] =
    Sync[F].delay(print(str))

  private def enterAction[F[_]: Sync]: F[Int] =
    Sync[F].delay(readInt())

  private def clear[F[_]: Sync]: F[Unit] = printStr("\u001b[2J")
  private def askAccept[F[_]: Sync]: F[Unit] =
    printStr("Нажмите enter для продолжения...") >> Sync[F]
      .delay(readLine())
      .as(())

  def menu[F[_]](client: SeafightClient[F])(implicit S: Sync[F]): F[Unit] =
    for {
      _      <- clear
      _      <- printLine("[1] создать новую игру")
      _      <- printLine("[2] присоединиться к игре")
      _      <- printLine("[3] выход")
      action <- printStr("Выберите действие: ") >> enterAction
      _      <- clear
      _ <- action match {
        case 1 =>
          createInviteLoop(client) >>= (inviteCtx =>
            setupShipsLoop(client)(inviteCtx.invite.id, inviteCtx.p))
        case 2 => printLine("Выбрано действие 2") >> menu(client)
        case 3 => printLine("Выбрано действие 3")
        case _ => printLine("Выберите действие из возможных") >> menu(client)
      }
    } yield ()

  def createInviteLoop[F[_]: Sync](
      client: SeafightClient[F]): F[PlayerWithInvite] =
    for {
      _     <- printStr("Первый игрок: ")
      rawP1 <- Sync[F].delay(readLine())
      _     <- printStr("Второй игрок: ")
      rawP2 <- Sync[F].delay(readLine())

      input = CreateInviteInput(Player(rawP1), Player(rawP2))
      result <- client.createInviteWithDecodeFailureInErrors(input)
      inviteCtx <- result.fold(
        printErrors(_) >> askAccept >> clear >> createInviteLoop(client),
        askCurrentPlayer(_) >>= (clear as _)
      )
    } yield inviteCtx

  private def askCurrentPlayer[F[_]: Sync](
      invite: InviteOutput): F[PlayerWithInvite] =
    for {
      _                    <- printStr("Вы первый игрок? [y/n]: ")
      currentPlayerIsFirst <- Sync[F].delay(readBoolean())
    } yield invite.toPlayerContext(currentPlayerIsFirst)

  def printErrors[F[_]: Sync](errors: NonEmptyChain[AppErrorOutput]): F[Unit] =
    printLine("Ошибки: ") >> printLine(errors.show)

  implicit val errorsShow: Show[NonEmptyChain[AppErrorOutput]] =
    (errors: NonEmptyChain[AppErrorOutput]) => errors.mkString_("\n")

  def setupShipsLoop[F[_]: Applicative](client: SeafightClient[F])(
      inviteId: UUID,
      p: Player)(implicit S: Sync[F]): F[Unit] = {
    def manualSetupLoop(ships: Set[Cell]): F[Set[Cell]] =
      for {
        _       <- printLine(viewShips(ships))
        _       <- printStr(s"Введите ${ships.size + 1}/20 клетку: ")
        rawCell <- S.delay(readLine())
        ships <- parseCell(rawCell.trim)
          .flatMap(checkUniqueShips(ships))
          .fold(
            printStr("Ошибка: ") >> printLine(_) >> askAccept >> clear >> manualSetupLoop(
              ships),
            cell => {
              val updatedShips = ships + cell
              clear >> (if (updatedShips.size == 20) updatedShips.pure[F]
                        else manualSetupLoop(updatedShips))
            }
          )
      } yield ships

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

    def fillShipsLoop: F[Set[Cell]] =
      for {
        _ <- printLine("[1] расставить корабли случайно")
        _ <- printLine("[2] расставить корабли вручную")

        action <- printStr("Выберите действие: ") >> enterAction
        _      <- clear
        ships <- action match {
          case 1 => Set.empty[Cell].pure[F] //todo: coming soon
          case 2 => manualSetupLoop(Set.empty[Cell])
          case _ =>
            printLine("Выберите действие из возможных") >> fillShipsLoop
        }
      } yield ships

    for {
      ships <- fillShipsLoop
      result <- client.addShipsWithDecodeFailureInErrors(inviteId, p)(
        ships.toSeq)
      _ <- result.fold(
        printErrors(_) >> setupShipsLoop(client)(inviteId, p),
        output =>
          printLine(s"Корабли: ${output.ships
            .map(cell => s"${cell.symbol.entryName}:${cell.digit.entryName}")
            .mkString(",")}")
      )
    } yield ()
  }

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
    case 3 => C
    case 4 => D
    case 5 => E
    case 6 => F
    case 7 => G
    case 8 => H
    case 9 => I
    case _ => throw new IllegalArgumentException
  }
}
