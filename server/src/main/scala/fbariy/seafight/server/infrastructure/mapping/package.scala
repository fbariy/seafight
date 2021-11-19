package fbariy.seafight.server.infrastructure

import doobie.postgres.circe.json.implicits.{pgDecoderGet, pgEncoderPut}
import doobie.util.meta.Meta
import fbariy.seafight.core.domain.{Cell, Turn}

package object mapping {
  implicit val cellMeta: Meta[Cell] = new Meta(pgDecoderGet, pgEncoderPut)
  implicit val seqCellMeta: Meta[Seq[Cell]] = new Meta(pgDecoderGet, pgEncoderPut)
  implicit val seqTurnMeta: Meta[Seq[Turn]] = new Meta(pgDecoderGet, pgEncoderPut)
}
