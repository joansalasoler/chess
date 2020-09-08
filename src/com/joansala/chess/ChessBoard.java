package com.joansala.chess;

/*
 * Copyright (C) 2015 Joan Sala Soler <contact@joansala.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.joansala.engine.Board;
import com.joansala.engine.Game;
import static com.joansala.chess.Chess.*;


/**
 * Represents a valid position and turn in a chess game.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class ChessBoard implements Board {

    /** Chess game object */
    private ChessGame game = new ChessGame();

    /** Distribution of the pieces on the board */
    private ChessPosition position;

    /** Which player moves on the position */
    private int turn;

    /** Position notation format pattern */
    private static Pattern positionPattern = Pattern.compile(
        "((?:(?:[PNBRQKpnbrqk1-8]{1,8})/){7}(?:[PNBRQKpnbrqk1-8]{1,8}))\\s+" +
        "([wb])\\s+(-|[KQkq]{1,4})\\s+(-|[a-h](?:3|6))\\s+" +
        "([0-9]+)\\s+([1-9][0-9]*)");

    /** Moves notation format pattern */
    private static Pattern movePattern = Pattern.compile(
        "((?:[a-h][1-8]){2}[NBRQnbrq]?)");


    /**
     * Instantiates a new board with the default position and turn
     */
    public ChessBoard() {
        this.turn = WHITE;
        this.position = new ChessPosition();
    }


    /**
     * Instantiates a new board with the specified position and turn.
     *
     * @param position  The position of the board
     * @param turn      The player that is to move
     *
     * @throws IllegalArgumentException  if the {@code postion} or
     *      {@code turn} parameters are not valid
     */
    public ChessBoard(ChessPosition position, int turn) {
        if (isValidTurn(turn) == false)
            throw new IllegalArgumentException(
                "Game turn is not a valid");

        if (isValidPosition(position) == false)
            throw new IllegalArgumentException(
                "Position representation is not valid");

        try {
            this.turn = turn;
            this.position = position.clone();
        } catch(Exception e) {}
    }


    /**
     * {@inheritDoc}
     */
    public ChessPosition position() {
        try {
            return position.clone();
        } catch(Exception e) {}

        return null;
    }


    /**
     * {@inheritDoc}
     */
    public int turn() {
        return turn;
    }


    /**
     * Returns true if the turn parameter is a valid player identifier.
     * A valid identifier must be either {@code Chess.WHITE} or
     * {@code Chess.BLACK}.
     *
     * @param turn  A player identifier
     * @return      {@code true} if turn is valid
     */
    private static boolean isValidTurn(int turn) {
        return turn == WHITE || turn == BLACK;
    }


    /**
     * Returns true if the array is a valid representation of a board
     * position. A valid position contains exactly fourty eight seeds
     * distributed in fourteen houses.
     *
     * @param position  An array representation of a position
     * @return          {@code true} if position is valid
     */
    private static boolean isValidPosition(ChessPosition position) {
        return position.isValid();
    }


    /**
     * Returns the piece sitting at the provided checker.
     *
     * @param bitboards     Bitboard representation of the chessboard
     * @param piece         Chessboard checker
     *
     * @return              Numeric piece representation
     */
    private static int pieceAt(int checker, long[] bitboards) {
        long bits = (1L << checker);

        if ((bits & bitboards[ALL_PIECES]) != 0L) {
            for (int i = 0; i < 12; i++) {
                if ((bits & bitboards[i]) != 0L)
                    return i;
            }
        }

        return NULL_PIECE;
    }


    /**
     * Returns a numberic representation of a piece.
     *
     * @param piece     Character piece representation
     * @return          Numeric representation
     */
    protected static int pieceIndex(char piece) {
        switch (piece) {
            case 'P': return WHITE_PAWN;
            case 'N': return WHITE_KNIGHT;
            case 'B': return WHITE_BISHOP;
            case 'R': return WHITE_ROOK;
            case 'Q': return WHITE_QUEEN;
            case 'K': return WHITE_KING;
            case 'p': return BLACK_PAWN;
            case 'n': return BLACK_KNIGHT;
            case 'b': return BLACK_BISHOP;
            case 'r': return BLACK_ROOK;
            case 'q': return BLACK_QUEEN;
            case 'k': return BLACK_KING;
        }

        return NULL_PIECE;
    }


    /**
     * Returns a character representation of a piece.
     *
     * @param piece     Numeric piece representation
     * @return          Character representation
     */
    protected static char pieceChar(int piece) {
        switch (piece) {
            case WHITE_PAWN:   return 'P';
            case WHITE_KNIGHT: return 'N';
            case WHITE_BISHOP: return 'B';
            case WHITE_ROOK:   return 'R';
            case WHITE_QUEEN:  return 'Q';
            case WHITE_KING:   return 'K';
            case BLACK_PAWN:   return 'p';
            case BLACK_KNIGHT: return 'n';
            case BLACK_BISHOP: return 'b';
            case BLACK_ROOK:   return 'r';
            case BLACK_QUEEN:  return 'q';
            case BLACK_KING:   return 'k';
        }

        return '-';
    }


    /**
     * {@inheritDoc}
     */
    public ChessBoard toBoard(String notation) {
        Matcher matcher = positionPattern.matcher(notation);

        if (matcher.matches() == false) {
            throw new IllegalArgumentException(
                "Position notation is not valid");
        }

        // Initialize the board an position

        ChessBoard board = new ChessBoard();
        ChessPosition pos = board.position;

        pos.castle = NULL_CASTLE;
        pos.passant = NULL_PASSANT;
        pos.clock = Integer.parseInt(matcher.group(5));
        pos.counter = Integer.parseInt(matcher.group(6));

        // Switch the turn if required

        if ("b".equals(matcher.group(2)))
            board.turn = BLACK;

        // En passant square

        String passant = matcher.group(4);

        if (!passant.equals("-")) {
            char r = passant.charAt(0);
            char c = passant.charAt(1);
            pos.passant = (byte) ((r - 'a') + 8 * (c - '1'));
        }

        // Bitboard for castling squares

        String castlings = matcher.group(3);

        for (int i = 0; i < castlings.length(); i++) {
            switch (castlings.charAt(i)) {
                case 'K': pos.castle |= 0x1; break;
                case 'Q': pos.castle |= 0x2; break;
                case 'k': pos.castle |= 0x4; break;
                case 'q': pos.castle |= 0x8; break;
            }
        }

        // Bitboards for the pieces

        String[] rows = matcher.group(1).split("/");
        Arrays.fill(pos.bitboards, 0x0L);

        for (int i = 0; i < 8; i++) {
            String row = rows[i];
            int count = 0;

            for (int n = 0; n < row.length(); n++) {
                char chr = row.charAt(n);
                int piece = pieceIndex(chr);

                if (piece == NULL_PIECE) {
                    count += (chr - '0');
                    continue;
                }

                int color = piece < 6 ? WHITE_PIECES : BLACK_PIECES;
                int checker = count + 8 * (7 - i);
                long bits = (1L << checker);

                pos.bitboards[ALL_PIECES] |= bits;
                pos.bitboards[piece] |= bits;
                pos.bitboards[color] |= bits;

                count++;
            }

            if (count != 8) {
                throw new IllegalArgumentException(
                    "Position notation is not valid");
            }
        }

        if (isValidPosition(pos) == false) {
            throw new IllegalArgumentException(
                "Position representation is not valid");
        }

        return board;
    }


    /**
     * {@inheritDoc}
     */
    public ChessBoard toBoard(Game game) {
        if (!(game instanceof ChessGame)) {
            throw new IllegalArgumentException(
                "Not a valid game object");
        }

        return new ChessBoard(
            (ChessPosition) game.position(), game.turn());
    }


    /**
     * Converts a move notation to a move identifier. This method does not
     * check for the validity of the move.
     *
     * @param notation  FEN notation
     * @param bitboards Position bitboards
     */
    private static int toMove(String notation, long[] bitboards) {
        Matcher matcher = movePattern.matcher(notation);

        if (matcher.matches() == false) {
            throw new IllegalArgumentException(
                "Not a valid move notation");
        }

        char fr = notation.charAt(0);
        char fc = notation.charAt(1);
        char tr = notation.charAt(2);
        char tc = notation.charAt(3);

        // Encode the provided move

        int xo = (fr - 'a') + 8 * (fc - '1');
        int xd = (tr - 'a') + 8 * (tc - '1');
        int pd = pieceAt(xo, bitboards);
        int pc = pieceAt(xd, bitboards);
        int flag = SIMPLE_MOVE;

        // Encode a capture if any

        if (pc != NULL_PIECE) {
            flag = CAPTURE_MOVE;
        } else if (pd == WHITE_PAWN) {
            if (xd - xo != 8 && xd - xo != 16)
                flag = PASSANT_MOVE;
        } else if (pd == BLACK_PAWN) {
            if (xo - xd != 8 && xo - xd != 16)
                flag = PASSANT_MOVE;
        }

        // Encode a promotion if any

        if (notation.length() == 5) {
            flag |= PROMOTE_MOVE;
            pd = pieceIndex(notation.charAt(4));
        }

        // Encode the castlings

        if (pd == WHITE_KING) {
            if (xo == E1 && (xd == C1 || xd == G1))
                flag = CASTLE_MOVE;
        }

        if (pd == BLACK_KING) {
            if (xo == E8 && (xd == C8 || xd == G8))
                flag = CASTLE_MOVE;
        }

        // Remove any unnecessary encoding for the pieces

        if (pc == NULL_PIECE)
            pc = 0x00;

        if (flag == SIMPLE_MOVE) {
            if (pd == BLACK_PAWN || pd == WHITE_PAWN)
                flag = PAWN_MOVE;
        }

        return flag | pc << 16 | xd << 10 | pd << 6 | xo;
    }


    /**
     * {@inheritDoc}
     */
    public int toMove(String notation) {
        return toMove(notation, position.bitboards);
    }


    /**
     * {@inheritDoc}
     */
    public int[] toMoves(String notation) {
        ChessPosition pos = null;
        String[] amoves = notation.split("\\s+");
        int[] moves = new int[amoves.length];
        int move = 0;

        game.ensureCapacity(amoves.length);
        game.setStart(position, turn);

        for (int i = 0; i < amoves.length; i++) {
            pos = game.position();
            move = toMove(amoves[i], pos.bitboards);
            game.makeMove(move);
            moves[i] = move;
        }

        return moves;
    }


    /**
     * {@inheritDoc}
     */
    public String toAlgebraic(int move) {
        int flag = move & 0xF00000;
        int from = move & 0x3F;
        int dest = move >> 10 & 0x3F;

        StringBuilder notation = new StringBuilder();

        notation.append((char) ('a' + from % 8));
        notation.append((char) ('1' + from / 8));
        notation.append((char) ('a' + dest % 8));
        notation.append((char) ('1' + dest / 8));

        if (flag == PROMOTE_MOVE || flag == PROMCAP_MOVE) {
            char piece = pieceChar(move >> 6 & 0xF);
            notation.append(piece);
        }

        return notation.toString();
    }


    /**
     * {@inheritDoc}
     */
    public String toAlgebraic(int[] moves) {
        StringBuilder notation = new StringBuilder();

        for (int i = 0; i < moves.length; i++) {
            int move = moves[i];

            notation.append(toAlgebraic(move));

            if (1 + i < moves.length)
                notation.append(' ');
        }

        return notation.toString();
    }


    /**
     * {@inheritDoc}
     */
    public String toNotation() {
        StringBuilder notation = new StringBuilder();
        long piecesBits = position.bitboards[ALL_PIECES];

        // Annotate the position

        int empty = 0;
        int offset = 0;

        for (int checker = 0; checker < 64; checker++) {
            long bits = (1L << checker);

            if (checker > 0 && checker % 8 == 0) {
                if (empty > 0) notation.insert(offset, empty);
                notation.insert(0, '/');
                empty = 0;
                offset = 0;
            }

            if ((piecesBits & bits) == 0) {
                empty++;
                continue;
            } else if (empty > 0) {
                notation.insert(offset, empty);
                empty = 0;
                offset++;
            }

            for (int piece = 0; piece < 12; piece++) {
                if ((position.bitboards[piece] & bits) != 0) {
                    notation.insert(offset, pieceChar(piece));
                    break;
                }
            }

            offset++;
        }

        if (empty > 0) notation.insert(offset, empty);

        // Annotate the turn

        notation.append(' ');
        notation.append(turn == WHITE ? "w" : "b");
        notation.append(' ');

        // Annotate the castling rights

        if (position.castle == 0) {
            notation.append('-');
        } else {
            if ((position.castle & 0x1) != 0)
                notation.append('K');
            if ((position.castle & 0x2) != 0)
                notation.append('Q');
            if ((position.castle & 0x4) != 0)
                notation.append('k');
            if ((position.castle & 0x8) != 0)
                notation.append('q');
        }

        // Annotate the en passant square

        if (position.passant == NULL_PASSANT) {
            notation.append(' ');
            notation.append('-');
        } else {
            char row = (char) ('a' + position.passant % 8);
            char col = (char) ('1' + position.passant / 8);
            notation.append(' ');
            notation.append(row);
            notation.append(col);
        }

        // Annotate the counters

        notation.append(' ');
        notation.append(position.clock);
        notation.append(' ');
        notation.append(position.counter);

        return notation.toString();
    }


    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        String[] ns = toNotation().split("\\s+", 2);
        StringBuilder sb = new StringBuilder();

        sb.append(ns[1]);
        sb.append('\n');

        for (int i = 0; i < ns[0].length(); i++) {
            char c = ns[0].charAt(i);

            if (c == '/') {
                sb.append('\n');
            } else if (c > '0' && c <= '9') {
                for (int n = 0; n < c - '0'; n++)
                    sb.append('·')
                      .append(' ');
            } else {
                sb.append(c)
                  .append(' ');
            };
        }

        return sb.toString();
    }
}
