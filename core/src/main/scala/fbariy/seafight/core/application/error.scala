package fbariy.seafight.core.application

import fbariy.seafight.core.domain.{Cell, Player}
import io.circe.Encoder

object error {
  sealed trait AppError

  sealed trait AuthError                        extends AppError
  case object MissingGameIdError                extends AuthError
  case object MissingPlayerError                extends AuthError
  case object MissingInviteIdError              extends AuthError
  case class BadFormatGameIdError(id: String)   extends AuthError
  case class BadFormatInviteIdError(id: String) extends AuthError
  case object NotFoundGameError                 extends AuthError
  case object NotFoundInviteError               extends AuthError

  case class NotCorrectShipsError(ships: Seq[Cell]) extends AppError
  case object SamePlayersError                      extends AppError
  case class PlayerCannotMakeMoveError(p: Player)   extends AppError
  case class GameOverError(winner: Player)          extends AppError
  case object EmptyPlayerError                      extends AppError
  case object GameAlreadyExist                      extends AppError
  case object MoveIsNotExistError                   extends AppError
  case object BackAlreadyRequestedError             extends AppError
  case object BackNotRequestedError                 extends AppError
  case object InitiatorCannotAcceptBackError        extends AppError

  object instances {
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
      case EmptyPlayerError =>
        AppErrorOutput("PLAYER_IS_EMPTY", "Имя игрока не может быть пустым")
      case GameAlreadyExist =>
        AppErrorOutput("GAME_ALREADY_EXIST", "Игра уже создана")
      case MoveIsNotExistError =>
        AppErrorOutput("MOVE_IS_NOT_EXIST", "Ход не существует")
      case BackAlreadyRequestedError =>
        AppErrorOutput("BACK_ALREADY_REQUESTED", "Возврат хода уже запрошен")
      case BackNotRequestedError =>
        AppErrorOutput("BACK_NOT_REQUESTED", "Возврат хода не был запрошен")
      case InitiatorCannotAcceptBackError =>
        AppErrorOutput("INITIATOR_CANNOT_ACCEPT_BACK",
          "Инициатор возврата не может принять его")
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
  }
}
