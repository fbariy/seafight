package fbariy.seafight.infrastructure.repository

import cats.Applicative
import cats.implicits._
import fbariy.seafight.application.invite.InviteRepository
import fbariy.seafight.domain.{Invite, Player}

import java.util.UUID
import scala.collection.mutable.ListBuffer

class InMemoryInviteRepository[F[_]](implicit val A: Applicative[F])
    extends InviteRepository[F] {
  private val invites: ListBuffer[Invite] = ListBuffer.empty

  override def add(invite: Invite): F[Invite] = {
    invites :+ invite
    invite.pure[F]
  }

  override def exists(inviteId: UUID): F[Boolean] =
    invites.exists(_.id == inviteId).pure[F]

  override def find(inviteId: UUID): F[Option[Invite]] =
    invites.find(_.id == inviteId).pure[F]

  override def findByIdAndPlayer(id: UUID, p: Player): F[Option[Invite]] =
    invites
      .find(invite => invite.id == id && (invite.p1 == p || invite.p2 == p))
      .pure[F]
}
