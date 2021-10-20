import fbariy.seafight.domain.Digit._
import fbariy.seafight.domain.Symbol._
import fbariy.seafight.infrastructure.client.PlayerState
import munit.FunSuite

class PlayerStateSuite extends FunSuite {
  test("Columns must be 'A B C D E F G H I'") {
    val state = PlayerState.fromString(
      """   || Q | K | C | D | E | F | G | H | A
        |========================================
        | 9 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 7 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 6 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 5 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        |""".stripMargin
    )

    assert(clue(state).isLeft)
  }

  test("Row numbers must be '9 8 7 6 5 4 3 2 1'") {
    val state = PlayerState.fromString(
      """   || A | B | C | D | E | F | G | H | I
        |========================================
        | 9 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        |""".stripMargin
    )

    assert(clue(state).isLeft)
  }

  test("Cell items must be one of '~ . X @'") {
    val state = PlayerState.fromString(
      """   || A | B | C | D | E | F | G | H | I
        |========================================
        | 9 || ~ | . | X | @ | @ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 7 || ~ | ~ | ~ | ~ | ? | ~ | ~ | ~ | ~
        | 6 || ~ | ~ | > | < | ~ | ~ | ~ | ~ | ~
        | 5 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 4 || ~ | ~ | ~ | = | = | @ | @ | ~ | ~
        | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        |""".stripMargin
    )

    assert(clue(state).isLeft)
  }

  test("State #1") {
    val state = PlayerState.fromString(
      """   || A | B | C | D | E | F | G | H | I
        |========================================
        | 9 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 7 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 6 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 5 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        |""".stripMargin
    )

    assertEquals(clue(state), Right(PlayerState(Seq(), Seq())))
  }

  test("State #2") {
    val state = PlayerState.fromString(
      """   || A | B | C | D | E | F | G | H | I
        |========================================
        | 9 || ~ | @ | @ | @ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 7 || ~ | ~ | @ | @ | ~ | ~ | ~ | ~ | ~
        | 6 || ~ | ~ | ~ | ~ | @ | ~ | ~ | ~ | ~
        | 5 || ~ | ~ | ~ | ~ | @ | ~ | ~ | ~ | ~
        | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        |""".stripMargin
    )

    assertEquals(
      state,
      Right(
        PlayerState(
          ships =
            Seq(D \ `9`, C \ `9`, B \ `9`, D \ `7`, C \ `7`, E \ `6`, E \ `5`),
          Seq())))
  }

  test("State #3") {
    val state = PlayerState.fromString(
      """   || A | B | C | D | E | F | G | H | I
        |========================================
        | 9 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 7 || ~ | ~ | . | . | . | ~ | ~ | ~ | ~
        | 6 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 5 || ~ | ~ | ~ | . | . | ~ | ~ | ~ | ~
        | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        |""".stripMargin
    )

    assertEquals(
      state,
      Right(
        PlayerState(Seq(),
                    kicks = Seq(E \ `7`, D \ `7`, C \ `7`, E \ `5`, D \ `5`))))
  }

  test("State #4") {
    val state = PlayerState.fromString(
      """   || A | B | C | D | E | F | G | H | I
        |========================================
        | 9 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 7 || ~ | ~ | X | X | X | ~ | ~ | ~ | ~
        | 6 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 5 || ~ | ~ | ~ | X | X | ~ | ~ | ~ | ~
        | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        |""".stripMargin
    )

    assertEquals(
      state,
      Right(
        PlayerState(ships = Seq(E \ `7`, D \ `7`, C \ `7`, E \ `5`, D \ `5`),
                    kicks = Seq(E \ `7`, D \ `7`, C \ `7`, E \ `5`, D \ `5`))))
  }

  test("State #5") {
    val state = PlayerState.fromString(
      """   || A | B | C | D | E | F | G | H | I
        |========================================
        | 9 || ~ | ~ | @ | @ | . | ~ | ~ | ~ | ~
        | 8 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 7 || ~ | ~ | X | X | X | ~ | ~ | ~ | ~
        | 6 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 5 || ~ | ~ | ~ | X | X | ~ | ~ | ~ | ~
        | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
        |""".stripMargin
    )

    assertEquals(
      state,
      Right(
        PlayerState(
          ships =
            Seq(D \ `9`, C \ `9`, E \ `7`, D \ `7`, C \ `7`, E \ `5`, D \ `5`),
          kicks = Seq(E \ `9`, E \ `7`, D \ `7`, C \ `7`, E \ `5`, D \ `5`))))
  }
}
