package fbariy.seafight.server.application.invite

import cats.data.ValidatedNec
import cats.effect.Sync
import cats.implicits._
import fbariy.seafight.core.application
import fbariy.seafight.core.application.{CreateInviteInput, InviteOutput}
import fbariy.seafight.core.application.error._
import fbariy.seafight.core.domain.{Invite, Player}

import java.util.UUID

final class CreateInviteHandler[F[_]: Sync](validator: CreateInviteValidator,
                                            inviteRepo: InviteRepository[F]) {
  def handle(
      input: CreateInviteInput): F[ValidatedNec[AppError, InviteOutput]] = {
    val trimmedInput = application.CreateInviteInput(
      Player(input.player1.name.trim),
      Player(input.player2.name.trim))
    import trimmedInput._

    for {
      validationRes <- Sync[F].delay(validate(player1, player2))
      result <- validationRes.traverse { _ =>
        for {
          id     <- Sync[F].delay(UUID.randomUUID())
          invite <- inviteRepo.add(Invite(id, player1, player2))
        } yield InviteOutput(invite)
      }
    } yield result
  }

  private def validate(p1: Player, p2: Player): ValidatedNec[AppError, Unit] =
    validator.playerIsNotEmpty(p1) |+|
      validator.playerIsNotEmpty(p2) |+|
      validator.playersAreNotSame(p1, p2)
}
