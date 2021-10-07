package fbariy.seafight.infrastructure.repository

import cats.effect.Bracket
import fbariy.seafight.application.game.GameRepository
import fbariy.seafight.domain.{Game, GameWithPlayers, Player, Turn}
import doobie.postgres.implicits._
import doobie.implicits._
import doobie.util.fragment.Fragment
import doobie.util.fragments.{setOpt, whereAndOpt}
import doobie.util.query.Query0
import doobie.util.transactor.Transactor
import doobie.util.update.Update0
import fbariy.seafight.infrastructure.mapping._

import java.util.UUID

class DoobieGameRepository[F[_]: Bracket[*[_], Throwable]](
    transactor: Transactor[F])
    extends GameRepository[F] {

  override def find(id: UUID): F[Option[GameWithPlayers]] =
    GameSql.find(Some(id), None).option.transact(transactor)

  override def findByIdAndPlayer(id: UUID,
                                 p: Player): F[Option[GameWithPlayers]] =
    GameSql.find(Some(id), Some(p)).option.transact(transactor)

  override def add(game: Game): F[Game] =
    GameSql.insert(game).run.map(_ => game).transact(transactor)

  override def updateTurns(id: UUID, turns: Seq[Turn]): F[Game] =
    GameSql
      .updateTurns(id, turns)
      .withUniqueGeneratedKeys[Game]("id", "p1_ships", "p2_ships", "turns")
      .transact(transactor)

  override def updateGame(id: UUID,
                          turns: Option[Seq[Turn]],
                          winner: Option[Player]): F[GameWithPlayers] =
    (for {
      _    <- GameSql.updateGame(id, turns, winner).run
      game <- GameSql.find(Some(id), None).unique
    } yield game).transact(transactor)
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
         select g.id, g.p1_ships, g.p2_ships, g.turns, i.p1, i.p2, g.winner
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

  def updateTurns(id: UUID, turns: Seq[Turn]): Update0 =
    sql"update game set turns = $turns where id = $id".update

  def updateGame(id: UUID,
                 turns: Option[Seq[Turn]],
                 winner: Option[Player]): Update0 =
    sql"update game ${setOpt(
      turns map (turns => fr"turns = $turns"),
      winner map (p => fr"winner = $p")
    )} where id = $id".update
}
