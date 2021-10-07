package fbariy.seafight.infrastructure.client

import cats.implicits._
import fbariy.seafight.domain.{Cell, Digit, Symbol}
import io.circe.{Decoder, Json}

import scala.annotation.tailrec
import scala.util.matching.Regex

case class PlayerState(ships: Seq[Cell] = Seq(), kicks: Seq[Cell] = Seq())
object PlayerState {
  val rawItemsReg: Regex   = "\\|\\s*(?<item>[~X@])\\s*".r
  val columnNameReg: Regex = "\\|\\s*(?<column>[A-I])\\s*".r
  val rowNumberReg: Regex  = "\\s*(?<row>[1-9])\\s*\\|".r

  val defaultState: String =
    """   || A | B | C | D | E | F | G | H | I
      |========================================
      | 9 || @ | @ | @ | @ | ~ | @ | @ | @ | ~
      | 8 || @ | @ | @ | ~ | @ | @ | ~ | @ | @
      | 7 || @ | @ | ~ | @ | ~ | @ | ~ | @ | ~
      | 6 || @ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
      | 5 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
      | 4 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
      | 3 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
      | 2 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
      | 1 || ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~ | ~
      |""".stripMargin

  def fromString: Either[String, PlayerState] = fromString(defaultState)

  def fromString(rawPlayerState: String): Either[String, PlayerState] =
    for {
      rows  <- validatePlayerStateSize(rawPlayerState)
      _     <- validateColumnNamesRow(rows.head)
      _     <- validateSeparator(rows(1))
      state <- parseRows(rows)
    } yield state

  private def validatePlayerStateSize(
                                       rawPlayerState: String): Either[String, List[String]] = {
    val rows = rawPlayerState.split("\n")

    if (rows.size != 11)
      Left(s"Таблица должна содержать 11 строк. Передано ${rows.size}")
    else Right(rows.toList)
  }

  private def validateColumnNamesRow(row: String): Either[String, Unit] =
    if (columnNameReg
      .findAllMatchIn(row)
      .map(_.group("column"))
      .mkString != "ABCDEFGHI")
      Left(
        "Недопустимый порядок или название колонок. Допускается: A B C F E F G H I")
    else Right(())

  private def validateSeparator(row: String): Either[String, Unit] =
    if (row.exists(_ != '='))
      Left("Разделитель должен состоять из символов '='")
    else Right(())

  private def parseRows(rows: List[String]): Either[String, PlayerState] =
    rows
      .slice(2, rows.size)
      .zipWithIndex
      .map {
        case row -> sequenceNumber =>
          parseRow(row, Math.abs(sequenceNumber - 9))
      }
      .sequence
      .map(_.foldLeft(PlayerState()) {
        case accum -> state =>
          PlayerState(accum.ships ++ state.ships, accum.kicks ++ state.kicks)
      })

  private def parseRow(row: String,
                       rawRowNumber: Int): Either[String, PlayerState] =
    for {
      rowNumber       <- parseRowNumber(row, rawRowNumber)
      items           <- parseRowItems(row)
      itemsWithColumn <- rowItemsWithColumn(items)
      state           <- itemsToPlayerState(rowNumber, itemsWithColumn, PlayerState())
    } yield state

  @tailrec
  private def itemsToPlayerState(
                                  row: Digit,
                                  items: Seq[(String, Symbol)],
                                  state: PlayerState): Either[String, PlayerState] =
    items match {
      case "~" -> _ :: rest =>
        itemsToPlayerState(row, rest, state)
      case "@" -> column :: rest =>
        itemsToPlayerState(row,
          rest,
          state.copy(ships = Cell(column, row) +: state.ships))
      case "X" -> column :: rest =>
        itemsToPlayerState(row,
          rest,
          PlayerState(Cell(column, row) +: state.ships,
            Cell(column, row) +: state.kicks))
      case Seq() => Right(state)
      case item -> _ :: _ =>
        Left(s"Допускается одно из значений ячейки: ~, X, @. Передано $item")
    }

  private def rowItemsWithColumn(
                                  items: Seq[String]): Either[String, Seq[(String, Symbol)]] = {
    ('A' to 'I').toList
      .map(column =>
        Decoder[Symbol].decodeJson(Json.fromString(column.toString)))
      .sequence
      .leftMap(_.message)
      .map { symbols =>
        items.zipWithIndex.map {
          case item -> index => item -> symbols(index)
        }
      }
  }

  private def parseRowNumber(row: String,
                             rowNumber: Int): Either[String, Digit] =
    rowNumberReg.findFirstMatchIn(row) match {
      case Some(matchRes) =>
        val specifiedNumber = matchRes.group("row").toInt
        val maybeValidRowNumber =
          if (specifiedNumber != rowNumber)
            Left(
              s"Указанный номер строки $specifiedNumber не соответствует его расположению $rowNumber")
          else Right(specifiedNumber)

        for {
          validRowNumber <- maybeValidRowNumber
          digit <- Decoder[Digit]
            .decodeJson(Json.fromString(validRowNumber.toString))
            .leftMap(_.message)
        } yield digit

      case None =>
        Left(
          "Номер строки не указан или указан некорректно, допускается число от 1 до 9")
    }

  private def parseRowItems(row: String): Either[String, Seq[String]] = {
    val items      = rawItemsReg.findAllMatchIn(row).map(_.group("item")).toList
    val countItems = items.size

    if (countItems != 9)
      Left(s"Допускается 9 элементов в строке, передано $countItems")
    else Right(items)
  }
}
