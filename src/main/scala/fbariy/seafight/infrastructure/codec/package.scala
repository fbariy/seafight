package fbariy.seafight.infrastructure

import cats.data.Validated.{Invalid, Valid}
import cats.data.{NonEmptyChain, ValidatedNec}
import cats.implicits._
import fbariy.seafight.application.AppErrorOutput
import fbariy.seafight.application.errors._
import fbariy.seafight.application.game.{GameOutput, TurnOutput}
import fbariy.seafight.application.invite.{CreateInviteInput, InviteOutput}
import fbariy.seafight.application.ship.AddShipsOutput
import fbariy.seafight.domain._
import io.circe._
import io.circe.generic.semiauto._

package object codec {
  implicit val playerDecoder: Decoder[Player] =
    (c: HCursor) => c.as[String].map(Player)

  implicit val playerEncoder: Encoder[Player] =
    (a: Player) => Json.fromString(a.name)

  implicit val cellEncoder: Encoder[Cell] =
    (obj: Cell) =>
      Json.obj(
        ("symbol", Json.fromString(obj.symbol match {
          case A => "A"
          case B => "B"
          case C => "C"
          case D => "D"
          case E => "E"
          case F => "F"
          case G => "G"
          case H => "H"
          case I => "I"
        })),
        ("digit", Json.fromInt(obj.digit match {
          case `1`   => 1
          case `2`   => 2
          case `3` => 3
          case `4`  => 4
          case `5`  => 5
          case `6`   => 6
          case `7` => 7
          case `8` => 8
          case `9`  => 9
        }))
    )
  implicit val cellDecoder: Decoder[Cell] =
    (c: HCursor) =>
      for {
        rawSymbol <- c.get[String]("symbol")
        rawDigit  <- c.get[Int]("digit")

        symbol <- rawSymbol match {
          case "A" => Right(A)
          case "B" => Right(B)
          case "C" => Right(C)
          case "D" => Right(D)
          case "E" => Right(E)
          case "F" => Right(F)
          case "G" => Right(G)
          case "H" => Right(H)
          case "I" => Right(I)
          case _ =>
            Left(
              DecodingFailure(
                s"Symbol must be one of the A-I value, given $rawSymbol",
                List()))
        }
        digit <- rawDigit match {
          case 1 => Right(`1`)
          case 2 => Right(`2`)
          case 3 => Right(`3`)
          case 4 => Right(`4`)
          case 5 => Right(`5`)
          case 6 => Right(`6`)
          case 7 => Right(`7`)
          case 8 => Right(`8`)
          case 9 => Right(`9`)
          case _ =>
            Left(
              DecodingFailure(
                s"Digit must be one of the 1-9 value, given $rawDigit",
                List()))
        }
      } yield Cell(symbol, digit)
  implicit val turnDecoder: Decoder[Turn]                       = deriveDecoder
  implicit val turnEncoder: Encoder[Turn]                       = deriveEncoder
  implicit val gameDecoder: Decoder[Game]                       = deriveDecoder
  implicit val gameEncoder: Encoder[Game]                       = deriveEncoder
  implicit val gameWithPlayersDecoder: Decoder[GameWithPlayers] = deriveDecoder
  implicit val gameWithPlayersEncoder: Encoder[GameWithPlayers] = deriveEncoder

  implicit val createInviteInputDecoder: Decoder[CreateInviteInput] =
    deriveDecoder
  implicit val createInviteInputEncoder: Encoder[CreateInviteInput] =
    deriveEncoder
  implicit val inviteOutputEncoder: Encoder[InviteOutput]     = deriveEncoder
  implicit val inviteOutputDecoder: Decoder[InviteOutput]     = deriveDecoder
  implicit val addShipsOutputEncoder: Encoder[AddShipsOutput] = deriveEncoder
  implicit val addShipsOutputDecoder: Decoder[AddShipsOutput] = deriveDecoder
  implicit val turnOutputEncoder: Encoder[TurnOutput]         = deriveEncoder
  implicit val turnOutputDecoder: Decoder[TurnOutput]         = deriveDecoder
  implicit val gameOutputEncoder: Encoder[GameOutput]         = deriveEncoder
  implicit val gameOutputDecoder: Decoder[GameOutput]         = deriveDecoder

  implicit val appErrorOutputDecoder: Decoder[AppErrorOutput] = deriveDecoder
  implicit val appErrorOutputEncoder: Encoder[AppErrorOutput] = deriveEncoder

  implicit def applicationErrorEncoder[E <: AppError]: Encoder[E] =
    (e: E) => Encoder[AppErrorOutput].apply(appErrorToJson(e))

  private val appErrorToJson: PartialFunction[AppError, AppErrorOutput] = {
    case e: AuthError => authErrorToJson(e)
    case NotCorrectShipsError(_) =>
      AppErrorOutput("NOT_CORRECT_SHIPS", "Не корректное размещение кораблей")
    case SamePlayersError =>
      AppErrorOutput("PLAYERS_ARE_SAME", "Игроки должны быть разными")
    case PlayerCannotMakeMoveError(_) =>
      AppErrorOutput("PLAYER_CANNOT_MAKE_MOVE", "Сейчас не ваш ход")
    case GameOverError(winner) =>
      AppErrorOutput("GAME_OVER", s"Игра окончена. Победитель: $winner")
  }

  private val authErrorToJson: PartialFunction[AuthError, AppErrorOutput] = {
    case MissingGameIdError =>
      AppErrorOutput("MISSING_GAME_ID", "Не указан GameId заголовок")
    case MissingPlayerError =>
      AppErrorOutput("MISSING_PLAYER", "Не указан Player заголовок")
    case MissingInviteIdError =>
      AppErrorOutput("MISSING_INVITE_ID", "Не указан InviteId заголовок")
    case BadFormatGameIdError(id) =>
      AppErrorOutput(
        "BAD_FORMAT_GAME_ID",
        s"Значение заголовка GameId должно быть в формате uuid v4, получено $id")
    case BadFormatInviteIdError(id) =>
      AppErrorOutput(
        "BAD_FORMAT_INVITE_ID",
        s"Значение заголовка InviteId должно быть в формате uuid v4, получено $id")
    case NotFoundGameError =>
      AppErrorOutput("NOT_FOUND_GAME", "Не найдена игра по переданным данным")
    case NotFoundInviteError =>
      AppErrorOutput("NOT_FOUND_INVITE",
                     "Не найден инвайт по переданным данным")
  }

  implicit def validatedNecEncoder[E: Encoder, A: Encoder]
    : Encoder[ValidatedNec[E, A]] = {
    case Valid(a) => Encoder[A].apply(a)
    case Invalid(e) =>
      Json.obj(
        "type"   -> Json.fromString("error"),
        "errors" -> Encoder[NonEmptyChain[E]].apply(e)
      )
  }

  implicit def validatedNecDecoder[E: Decoder, A: Decoder]
    : Decoder[ValidatedNec[E, A]] =
    (c: HCursor) =>
      (c.get[String]("type"), c.get[Json]("errors")).tupled match {
        case Left(_) => Decoder[A].apply(c).map(_.valid.toValidatedNec)
        case Right(_ -> errors) =>
          Decoder[NonEmptyChain[E]].decodeJson(errors).map(_.invalid)
    }
}
