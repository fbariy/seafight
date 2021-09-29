package fbariy.seafight.infrastructure.repository

import cats.Applicative
import cats.implicits._
import fbariy.seafight.application.ship.{PlayerShips, ShipsRepository}
import fbariy.seafight.domain.{Cell, Invite, Player}

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import scala.jdk.CollectionConverters.ConcurrentMapHasAsScala

class InMemoryShipsRepository[F[_]: Applicative] extends ShipsRepository[F] {
  private val preparation =
    new ConcurrentHashMap[UUID, (Option[PlayerShips], Option[PlayerShips])].asScala

  override def add(invite: Invite,
                   p: Player,
                   first: Boolean,
                   ships: Seq[Cell]): F[Seq[Cell]] = {
    val pShips = PlayerShips(p, ships)

    preparation.updateWith(invite.id) {
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

    ships.pure[F]
  }

  override def release(
      invite: Invite): F[Option[(PlayerShips, PlayerShips)]] = {
    (for {
      pairMaybeShips <- preparation.get(invite.id)
      maybePairShips <- pairMaybeShips.tupled
      _              <- preparation.remove(invite.id)
    } yield maybePairShips).pure[F]
  }
}
