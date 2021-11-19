package fbariy.seafight.server.application.invite

import fbariy.seafight.core.domain.{Invite, Player}

import java.util.UUID

trait InviteRepository[F[_]] {
  def add(invite: Invite): F[Invite]

  def exists(inviteId: UUID): F[Boolean]

  def find(inviteId: UUID): F[Option[Invite]]

  def findByIdAndPlayer(id: UUID, p: Player): F[Option[Invite]]
}
