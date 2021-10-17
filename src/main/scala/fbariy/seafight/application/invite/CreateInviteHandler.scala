package fbariy.seafight.application.invite

import cats.data.Validated.{Invalid, Valid}
import cats.data.{Validated, ValidatedNec}
import cats.effect.Sync
import cats.implicits._
import fbariy.seafight.application.error._
import fbariy.seafight.domain.{Invite, Player}

import java.util.UUID

class CreateInviteHandler[F[_]: Sync](validator: CreateInviteValidator,
                                      inviteRepo: InviteRepository[F]) {
  def handle(input: CreateInviteInput)
    : F[ValidatedNec[AppError, InviteOutput]] = {
    val trimmedInput = CreateInviteInput(Player(input.player1.name.trim),
                                         Player(input.player2.name.trim))
    import trimmedInput._

    Sync[F]
      .delay(
        validator.playerIsNotEmpty(player1) |+|
          validator.playerIsNotEmpty(player2) |+|
          validator.playersAreNotSame(player1, player2)
      )
      .flatMap {
        case Valid(_) =>
          for {
            id     <- Sync[F].delay(UUID.randomUUID())
            invite <- inviteRepo.add(Invite(id, player1, player2))
          } yield Valid(InviteOutput(invite))
        case Invalid(e) => e.invalid[InviteOutput].pure[F]
      }
  }
}
