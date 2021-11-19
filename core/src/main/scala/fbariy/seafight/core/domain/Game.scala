package fbariy.seafight.core.domain

import java.util.UUID

case class Game(id: UUID,
                p1Ships: Seq[Cell],
                p2Ships: Seq[Cell],
                turns: Seq[Turn])
