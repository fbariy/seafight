package fbariy.seafight.application.invite

import cats.data.Validated.{Invalid, Valid}
import cats.data.{Validated, ValidatedNec}
import cats.effect.Sync
import cats.implicits._
import fbariy.seafight.application.errors._
import fbariy.seafight.domain.Invite

import java.util.UUID

class CreateInviteHandler[F[_]: Sync](validator: CreateInviteValidator,
                                      inviteRepo: InviteRepository[F]) {
  def handle(input: CreateInviteInput)
    : F[ValidatedNec[SamePlayersError.type, InviteOutput]] = {
    import input._

    Sync[F]
      .delay(validator.playersAreNotSame(player1, player2))
      .flatMap {
        case Valid(_) =>
          for {
            id     <- Sync[F].delay(UUID.randomUUID())
            invite <- inviteRepo.add(Invite(id, player1, player2))
          } yield Valid(invite)
        case Invalid(_) =>
          Validated
            .invalidNec[SamePlayersError.type, InviteOutput](SamePlayersError)
            .pure[F]
      }
  }
}
