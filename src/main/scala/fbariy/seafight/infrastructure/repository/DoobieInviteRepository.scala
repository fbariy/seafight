package fbariy.seafight.infrastructure.repository

import cats.effect.Bracket
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import fbariy.seafight.domain.{Invite, Player}
import doobie.postgres.implicits._
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.fragments.whereAndOpt
import doobie.util.query.Query0
import fbariy.seafight.application.invite.InviteRepository

import java.util.UUID

class DoobieInviteRepository[F[_]: Bracket[*[_], Throwable]](val xa: Transactor[F])
    extends InviteRepository[F] {

  override def add(invite: Invite): F[Invite] =
    InviteSql.insert(invite).run.map(_ => invite).transact(xa)

  override def exists(inviteId: UUID): F[Boolean] =
    InviteSql.countById(inviteId).map(_ > 0).unique.transact(xa)

  override def find(id: UUID): F[Option[Invite]] =
    InviteSql.find(Some(id), None).option.transact(xa)

  override def findByIdAndPlayer(id: UUID, p: Player): F[Option[Invite]] =
    InviteSql.find(Some(id), Some(p)).option.transact(xa)
}

private object InviteSql {
  def insert(invite: Invite): Update0 =
    sql"insert into invite(id, p1, p2) values (${invite.id}, ${invite.p1}, ${invite.p2})".update

  def countById(inviteId: UUID): Query0[Int] =
    sql"select count(*) from invite where id = $inviteId".query

  def find(id: Option[UUID], anyPlayer: Option[Player]): Query0[Invite] =
    (findFr ++ whereAndOpt(
      id.map(id => fr"id = $id"),
      anyPlayer.map(p => fr"p1 = $p or p2 = $p")
    )).query

  val findFr: Fragment =
    fr"select * from invite"
}
