package fbariy.seafight.server.infrastructure.repository

import cats.effect.Sync
import cats.implicits._
import fbariy.seafight.server.application.ship.{PlayerShips, ShipsRepository}
import fbariy.seafight.core.domain.{Cell, Invite, Player}

import java.util.UUID
import scala.collection.concurrent.TrieMap

class InMemoryShipsRepository[F[_]: Sync] extends ShipsRepository[F] {
  private val playersShips
    : TrieMap[UUID, (Option[PlayerShips], Option[PlayerShips])] = TrieMap.empty

  override def add(invite: Invite,
                   p: Player,
                   first: Boolean,
                   ships: Seq[Cell]): F[Seq[Cell]] =
    Sync[F].delay {
      val pShips = PlayerShips(p, ships)

      playersShips.updateWith(invite.id) {
        case Some(value) =>
          Some(
            if (first) (Some(pShips), value._2)
            else (value._1, Some(pShips))
          )
        case None =>
          Some(
            if (first) (Some(pShips), None)
            else (None, Some(pShips))
          )
      }

      ships
    }

  override def release(invite: Invite): F[Option[(PlayerShips, PlayerShips)]] =
    Sync[F].delay {
      for {
        pairMaybeShips <- playersShips.get(invite.id)
        maybePairShips <- pairMaybeShips.tupled
        _              <- playersShips.remove(invite.id)
      } yield maybePairShips
    }

  override def has(inviteId: UUID, p: Player): F[Boolean] =
    Sync[F].delay {
      playersShips.get(inviteId).exists {
        case (Some(PlayerShips(player, _)), _) if p == player => true
        case (_, Some(PlayerShips(player, _))) if p == player => true
        case _ => false
      }
    }
}
