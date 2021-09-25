package fbariy.seafight.infrastructure.repository

import cats.effect.Bracket
import fbariy.seafight.application.game.GameRepo
import fbariy.seafight.domain.{Game, GameWithPlayers, Player}
import doobie.postgres.implicits._
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.fragments.whereAndOpt
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import fbariy.seafight.infrastructure.mapping._

import java.util.UUID

class DoobieGameRepo[F[_]: Bracket[*[_], Throwable]](transactor: Transactor[F])
    extends GameRepo[F] {

  override def find(id: UUID): F[Option[GameWithPlayers]] =
    GameSql.find(Some(id), None).option.transact(transactor)

  override def findByIdAndPlayer(id: UUID,
                                 p: Player): F[Option[GameWithPlayers]] =
    GameSql.find(Some(id), Some(p)).option.transact(transactor)

  override def add(game: Game): F[Game] =
    GameSql.insert(game).run.map(_ => game).transact(transactor)
}

private object GameSql {
  def find(id: Option[UUID],
           anyPlayer: Option[Player]): Query0[GameWithPlayers] =
    (findFr ++ whereAndOpt(
      id.map(id => fr"g.id = $id"),
      anyPlayer.map(p => fr"i.p1 = $p or i.p2 = $p")
    )).query

  private val findFr: Fragment =
    fr"""
         select g.id, g.p1_ships, g.p2_ships, g.turns, i.p1, i.p2
         from game g
         join invite i on i.id = g.invite_id
    """

  def insert(game: Game): Update0 =
    sql"""
          insert into game 
          (id, invite_id, p1_ships, p2_ships, turns)
          values 
          (${game.id}, ${game.id}, ${game.p1Ships}, ${game.p2Ships}, ${game.turns})
    """.update
}
