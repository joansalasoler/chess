package com.joansala.chess;

/*
 * Copyright (C) 2015-2016 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.engine.Engine;
import com.joansala.engine.Game;
import static com.joansala.chess.Chess.*;

import java.io.FileInputStream;
import java.io.ObjectInputStream;


/**
 * Implements a simple chess game logic using bitboards.
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public class ChessGame implements Game {

    /** Maximum possible score */
    public static final int MAX_SCORE = Integer.MAX_VALUE; /* TODO */

    /** The maximum number of moves this object can store */
    public static final int MAX_CAPACITY = Integer.MAX_VALUE >> 4;

    /** Default capacity for this object */
    private static final int DEFAULT_CAPACITY = 254;

    /** Capacity increases at least this value each time */
    private static final int CAPACITY_INCREMENT = 126;

    /** De Bruijn sequence multiplier */
    private static final long DEBRUIJN = 0X03F79D71B4CB0A89L;

    /* Current capacity of this object */

    private int capacity;                  // Current capacity of this object

    /* Current state indices */

    private int index = -1;                // Current move index
    private int advance = -1;              // Last capture or pawn move index
    private int next = 0;                  // Next legal move to return index
    private int last = -1;                 // Last generated legal move index

    /* Current game state */

    private int turn = WHITE;              // Player to move
    private int move = NULL_MOVE;          // Last move done
    private int color = WHITE_PIECES;      // Player to move color
    private byte castle = START_CASTLE;    // Current castling rights
    private byte passant = NULL_PASSANT;   // En passant square
    private byte status = GEN_PROMOTIONS;  // Move generation status
    private long evasion = ALL_MATCH;      // Evasions mask
    private long hash;                     // Current hash code

    /* History for this game */

    private int[] moves;                   // Performed moves
    private int[] advances;                // Half-move clocks
    private int[] nexts;                   // Next legal move
    private int[] lasts;                   // Last legal move
    private int[] legals;                  // Legal moves
    private byte[] castles;                // Castling rights
    private byte[] passants;               // En passant squares
    private byte[] statuses;               // Move generation statuses
    private long[] hashes;                 // Hash code history
    private long[] evasions;               // Evasions history

    /* Current game position */

    long[] bitboards;                      // Position representation

    /* King checkers */

    int target = E1;                       // Player King square
    int _target = E8;                      // Rival King square

    /* State bitboards */

    long free;                             // Free checkers
    long occupied;                         // Occupied checkers
    long loyal;                            // Occupied player checkers
    long enemy;                            // Occupied rival checkers

    /* Current player bitboards */

    long king;                             // Player King
    long pawns;                            // Player Pawns
    long knights;                          // Player Knights
    long queens;                           // Player Queens
    long bishops;                          // Player Bishops
    long rooks;                            // Player Rooks
    long kattacks;                         // Player King attacks

    /* Opponent player bitboards */

    long _king;                            // Rival King
    long _pawns;                           // Rival Pawns
    long _knights;                         // Rival Knights
    long _queens;                          // Rival Queens
    long _bishops;                         // Rival Bishops
    long _rooks;                           // Rival Rooks
    long _slidersB;                        // Rival Bishops and Queens
    long _slidersR;                        // Rival Rooks and Queens
    long _kattacks;                        // Rival King attacks


    /**
     * Instantiates a new {@code ChessGame} object with the default
     * start position and capacity.
     */
    public ChessGame() {
        this(DEFAULT_CAPACITY);
    }


    /**
     * Instantiates a new {@code ChessGame} object with the default
     * start position and the specified capacity.
     *
     * @param capacity  Number of moves this game can store initially
     */
    public ChessGame(int capacity) {
        this.capacity = capacity;
        this.moves = new int[capacity];
        this.advances = new int[capacity];
        this.nexts = new int[capacity];
        this.lasts = new int[capacity];
        this.castles = new byte[capacity];
        this.passants = new byte[capacity];
        this.statuses = new byte[capacity];
        this.hashes = new long[capacity];
        this.evasions = new long[capacity];
        this.legals = new int[capacity << 8];
        this.bitboards = new long[15];

        System.arraycopy(START_BITBOARDS, 0, bitboards, 0, 15);
        updateBitboards();

        this.hash = computeHash();
    }


    /**
     * {@inheritDoc}
     */
    public int length() {
        return 1 + index;
    }


    /**
     * {@inheritDoc}
     */
    public int[] moves() {
        if (index == -1)
            return null;

        int[] moves = new int[1 + index];
        System.arraycopy(this.moves, 0, moves, 0, index);
        moves[index] = this.move;

        return moves;
    }


    /**
     * {@inheritDoc}
     */
    public int turn() {
        return turn;
    }


    /**
     * {@inheritDoc}
     */
    public ChessPosition position() {
        ChessPosition position = new ChessPosition();

        position.passant = passant;
        position.castle = castle;
        position.clock = index - advance;
        position.counter = 1 + length() / 2;
        System.arraycopy(bitboards, 0, position.bitboards, 0, 15);

        return position;
    }


    /**
     * Sets a new position and turn as the initial board for the game.
     *
     * @param position  An array representation of a position
     * @param turn      The player to move on the position. Must be
     *                  either {@code Chess.WHITE} or {@code Chess.BLACK}.
     *
     * @throws IllegalArgumentException  if {@code turn} is not valid or
     *      {@code postion} is not a valid position representation
     */
    public void setStart(Object position, int turn) {
        if (turn != WHITE && turn != BLACK)
            throw new IllegalArgumentException(
                "Game turn is not a valid");

        if (!(position instanceof ChessPosition))
            throw new IllegalArgumentException(
                "Not a valid ChessPosition object");

        ChessPosition pos = (ChessPosition) position;
        System.arraycopy(pos.bitboards, 0, this.bitboards, 0, 15);

        this.index = -1;
        this.turn = turn;
        this.color = (turn == WHITE) ? WHITE_PIECES : BLACK_PIECES;
        this.move = NULL_MOVE;
        this.passant = pos.passant;
        this.advance = -1 - pos.clock;
        this.castle = pos.castle;
        this.next = 0;
        this.last = -1;

        updateBitboards();

        this.hash = computeHash();
        this.evasion = evasionMask();
        this.status = (evasion == ALL_CLEAR) ?
            GEN_KINGMOVES : GEN_PROMOTIONS;
    }


    /**
     * {@inheritDoc}
     */
    public void endMatch() {
        // Does nothing
    }


    /**
     * {@inheritDoc}
     */
    public boolean hasEnded() {
        // The game is drawn if checkmate cannot occur (FIDE 9.7)
        // Only the combinations KvK, KBvK, KNvK and KB+vKB+ with bishops
        // on the same color are accounted here.

        if (king == loyal) {
            if ((_king | _bishops) == enemy &&
               (_bishops & (_bishops - 1L)) == 0L)
                return true;

            if ((_king | _knights) == enemy &&
               (_knights & (_knights - 1L)) == 0L)
                return true;
        } else if (_king == enemy) {
            if ((king | bishops) == loyal &&
               (bishops & (bishops - 1L)) == 0L)
                return true;

            if ((king | knights) == loyal &&
               (knights & (knights - 1L)) == 0L)
                return true;
        } else if ((king | bishops) == loyal &&
                   (_king | _bishops) == enemy) {
            final long pieces = bishops | _bishops;

            if ((pieces & WHITE_CHECKERS) == 0L ||
                (pieces & BLACK_CHECKERS) == 0L)
                return true;
        }

        // Completed 75 moves without any advance (FIDE 9.6b)

        if (index - advance >= 150)
            return true;

        // Position repeated in 5 consecutive moves (FIDE 9.6a)

        final int first = index - 8;

        if (first >= 0) {
            for (int n = index - 1; n >= first; n -= 2) {
                if (hashes[n] != hash)
                    break;

                if (n == first)
                    return true;
            }
        }

        // Checkmate or stalemate occurred

        return !hasLegalMoves();
    }


    /**
     * {@inheritDoc}
     */
    public int winner() {
        return inCheck() && hasEnded() ? -turn : DRAW;
    }


    /**
     * {@inheritDoc}
     */
    public int score() { /* TODO */
        // Fifty-move rule and threefold repetition

        if (index - advance >= 100 || isRepetition())
            return DRAW_SCORE;

        /* TODO: Heuristic evaluation */

        return DRAW_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    public int outcome() {
        return inCheck() ? turn * MAX_SCORE : DRAW_SCORE;
    }


    /**
     * {@inheritDoc}
     */
    public long hash() {
        return this.hash;
    }


    /**
     * Computes a Zobrist hash code for the current position.
     *
     * @see ChessGame#hash
     * @return  The hash code for the current position and turn
     */
    private long computeHash() {
        long hash = 0L;

        for (int piece = 0; piece < 12; piece++) {
            long bits = bitboards[piece];

            while (bits != 0L) {
                int checker = checker(bits & -bits);
                hash ^= ZOBRIST[piece][checker];
                bits &= bits - 1L;
            }
        }

        hash ^= castle;
        hash ^= passant;
        hash ^= color;

        return hash;
    }


    /**
     * {@inheritDoc}
     */
    public boolean isLegal(int move) {
        if (isValid(move)) {
            final int[] legals = legalMoves();

            for (int i = 0; i < legals.length; i++) {
                if (move == legals[i])
                    return true;
            }
        }

        return false;
    }


    /**
     * Checks if a move can be performed on the current position.
     *
     * This method assumes a valid move representation and checks that
     * the move can be performed; it does not validate its legality.
     *
     * @param move  Valid move representation
     * @return      {@code true} if the move is valid
     */
    protected final boolean isValid(int move) {
        final int piece = move >> 6 & 0xF;
        final long fromMask = 1L << (move & 0x3F);

        if ((bitboards[piece] & fromMask) != 0L) {
            final long toMask = 1L << (move >> 10 & 0x3F);

            if (isCapture(move)) {
                if (!isPassant(move)) {
                    final int capture = move >> 16 & 0xF;

                    if ((bitboards[capture] & toMask) != 0L)
                        return true;
                } else if (toMask == (1L << passant)) {
                    return true;
                }
            } else if ((free & toMask) != 0L) {
                return true;
            }
        }

        return false;
    }


    /**
     * Checks if at least one legal move can be performed for the
     * current position and turn
     *
     * @return  {@code true} if a legal move can be performed for the
     *          player to move or {@code false} otherwise
     */
    public boolean hasLegalMoves() {
        // Search for a legal King move

        final long bits = kattacks & ~_kattacks & ~loyal;

        for (long d = bits; d != 0L; d &= d - 1L) {
            if (isLegalKingMove(checker(d & -d)))
                return true;
        }

        if (evasion == ALL_CLEAR)
            return false;

        // Return true if there are generated moves

        if (last >= next)
            return true;

        // Generate the first set of moves

        if (nextMove() != NULL_MOVE) {
            next--;
            return true;
        }

        return false;
    }


    /**
     * Checks if the current position is a three-fold repetition.
     *
     * @return  {@code true} if the last performed move lead to a
     *          position repetition; {@code false} otherwise
     */
    public boolean isRepetition() {
        boolean found = false;

        for (int n = index - 1; n > advance; n -= 2) {
            if (castles[n] != castle)
                break;

            if (hashes[n] != hash)
                continue;

            if (!found) {
                found = true;
                continue;
            }

            return true;
        }

        return false;
    }


    /**
     * Returns if the current player is in check.
     *
     * @return          {@code true} if in check
     */
    public boolean inCheck() {
        return this.evasion != ALL_MATCH;
    }


    /**
     * Returns if the given checker is attacked by an opponent piece.
     *
     * @param checker   Checker identifier
     * @return          {@code true} if attacked
     */
    private final boolean isAttacked(int checker) {
        return (_pawns & pawnAttacks(checker, color)) != 0L ||
               (_knights & knightAttacks(checker)) != 0L ||
               (_king & kingAttacks(checker)) != 0L ||
               (_slidersB & bishopAttacks(checker)) != 0L ||
               (_slidersR & rookAttacks(checker)) != 0L;
    }


    /**
     * Returns if the King can move to the given checker.
     *
     * This method checks that the King is not in check after moving to the
     * given square, but it does not test if the opponent King is attacking
     * that same checker.
     *
     * @param checker   Checker identifier
     * @return          {@code true} if attacked
     */
    private final boolean isLegalKingMove(int checker) {
        occupied ^= king;

        final boolean attacked =
            (_pawns & pawnAttacks(checker, color)) != 0L ||
            (_knights & knightAttacks(checker)) != 0L ||
            (_slidersB & bishopAttacks(checker)) != 0L ||
            (_slidersR & rookAttacks(checker)) != 0L;

        occupied ^= king;

        return !attacked;
    }


    /**
     * {@inheritDoc}
     */
    public void makeMove(int move) {
        // Copy old position and staus to history

        this.index++;
        this.moves[index] = this.move;
        this.advances[index] = this.advance;
        this.castles[index] = this.castle;
        this.passants[index] = this.passant;
        this.statuses[index] = this.status;
        this.hashes[index] = this.hash;
        this.evasions[index] = this.evasion;
        this.nexts[index] = this.next;
        this.lasts[index] = this.last;

        // Perform the move and update the state

        switch (move & 0xF00000) {
            case CAPTURE_MOVE:
                makeCapture(move);
                break;
            case SIMPLE_MOVE:
                makeSimpleMove(move);
                break;
            case PAWN_MOVE:
                makePawnMove(move);
                break;
            case CASTLE_MOVE:
                makeCastling(move);
                break;
            case PROMOTE_MOVE:
                makePromotion(move);
                break;
            case PASSANT_MOVE:
                makePassant(move);
                break;
            default:
                makePromcap(move);
        }

        this.turn = -this.turn;

        updateBitboards();

        this.move = move;
        this.hash ^= 0x3;
        this.color ^= 0x3;
        this.next = 0;
        this.last = -1;
        this.evasion = evasionMask();
        this.status = (evasion == ALL_CLEAR) ?
            GEN_KINGMOVES : GEN_PROMOTIONS;
    }


    /**
     * {@inheritDoc}
     */
    public void unmakeMove() {
        // Restore the position

        this.color ^= 0x3;
        this.turn = -this.turn;

        switch (move & 0xF00000) {
            case CAPTURE_MOVE:
                unmakeCapture(move);
                break;
            case SIMPLE_MOVE:
                unmakeSimpleMove(move);
                break;
            case PAWN_MOVE:
                unmakePawnMove(move);
                break;
            case CASTLE_MOVE:
                unmakeCastling(move);
                break;
            case PROMOTE_MOVE:
                unmakePromotion(move);
                break;
            case PASSANT_MOVE:
                unmakePassant(move);
                break;
            default:
                unmakePromcap(move);
        }

        // Restore current history

        this.evasion = this.evasions[index];
        this.hash = this.hashes[index];
        this.status = this.statuses[index];
        this.passant = this.passants[index];
        this.castle = this.castles[index];
        this.advance = this.advances[index];
        this.move = this.moves[index];
        this.next = this.nexts[index];
        this.last = this.lasts[index];

        this.index--;

        updateBitboards();
    }


    /**
     * {@inheritDoc}
     */
    public int nextMove() {
        return (turn == WHITE) ?
            nextMoveWhite() : nextMoveBlack();
    }


    /**
     * {@inheritDoc}
     */
    public int[] legalMoves() {
        int cnext = this.next;

        // Generate from the first move

        this.next = (1 + index) << 8;
        this.last = this.next - 1;

        // Generate all the legal moves

        if (turn == WHITE) {
            if (evasion == ALL_CLEAR) {
                genKingMovesWhite();
            } else {
                genPromotionsWhite();
                genCapturesWhite();
                genOtherMovesWhite();
                genUnderpromsWhite();
            }
        } else {
            if (evasion == ALL_CLEAR) {
                genKingMovesBlack();
            } else {
                genPromotionsBlack();
                genCapturesBlack();
                genOtherMovesBlack();
                genUnderpromsBlack();
            }
        }

        // Return the generated moves array

        int length = 1 + last - next;
        int[] moves = null;

        if (length > 0) {
            moves = new int[length];
            System.arraycopy(legals, next, moves, 0, length);
        }

        // Recover the next move status

        this.next = cnext;

        return moves;
    }


    /**
     * Returns the next legal move on the current position for the white
     * player.
     *
     * @return  A legal move identifier or {@code Game.NULL_MOVE} if no
     *          more moves can be returned
     */
    private final int nextMoveWhite() {
        if (next <= last)
            return legals[next++];

        switch (status) {
            case GEN_KINGMOVES:
                status = GEN_FINALIZED;
                next = (1 + index) << 8;
                last = next - 1;
                genKingMovesWhite();
                if (next <= last)
                return legals[next++];
                break;
            case GEN_PROMOTIONS:
                status = GEN_CAPTURES;
                next = (1 + index) << 8;
                last = next - 1;
                genPromotionsWhite();
                if (next <= last)
                return legals[next++];
            case GEN_CAPTURES:
                status = GEN_OTHERMOVES;
                genCapturesWhite();
                if (next <= last)
                return legals[next++];
            case GEN_OTHERMOVES:
                status = GEN_UNDERPROMS;
                genOtherMovesWhite();
                if (next <= last)
                return legals[next++];
            case GEN_UNDERPROMS:
                status = GEN_FINALIZED;
                if (last < 0) break;
                genUnderpromsWhite();
                if (next <= last)
                return legals[next++];
        }

        return NULL_MOVE;
    }


    /**
     * Returns the next legal move on the current position for the black
     * player.
     *
     * @return  A legal move identifier or {@code Game.NULL_MOVE} if no
     *          more moves can be returned
     */
    private final int nextMoveBlack() {
        if (next <= last)
            return legals[next++];

        switch (status) {
            case GEN_KINGMOVES:
                status = GEN_FINALIZED;
                next = (1 + index) << 8;
                last = next - 1;
                genKingMovesBlack();
                if (next <= last)
                return legals[next++];
                break;
            case GEN_PROMOTIONS:
                status = GEN_CAPTURES;
                next = (1 + index) << 8;
                last = next - 1;
                genPromotionsBlack();
                if (next <= last)
                return legals[next++];
            case GEN_CAPTURES:
                status = GEN_OTHERMOVES;
                genCapturesBlack();
                if (next <= last)
                return legals[next++];
            case GEN_OTHERMOVES:
                status = GEN_UNDERPROMS;
                genOtherMovesBlack();
                if (next <= last)
                return legals[next++];
            case GEN_UNDERPROMS:
                status = GEN_FINALIZED;
                if (last < 0) break;
                genUnderpromsBlack();
                if (next <= last)
                return legals[next++];
        }

        return NULL_MOVE;
    }


    /**
     * Generates promotion moves for the white player and stores them on the
     * global moves array.
     */
    private final void genPromotionsWhite() {
        final long pawnsR7M = pawns & R7_MATCH;

        if (pawnsR7M == 0L)
            return;

        // Pawn capture & promote moves

        long ep = (pawnsR7M & CH_CLEAR) << 7 & enemy;

        for (int p = BLACK_QUEEN; ep != 0L && p >= BLACK_PAWN; p--) {
            final long bits = ep & bitboards[p];

            if (bits != 0L) {
                unpackPawnMoves(-7, bits, p << 16 |
                    WHITE_QUEEN << 6 | PROMCAP_MOVE);
                ep ^= bits;
            }
        }

        long wp = (pawnsR7M & CA_CLEAR) << 9 & enemy;

        for (int p = BLACK_QUEEN; wp != 0L && p >= BLACK_PAWN; p--) {
            final long bits = wp & bitboards[p];

            if (bits != 0L) {
                unpackPawnMoves(-9, bits, p << 16 |
                    WHITE_QUEEN << 6 | PROMCAP_MOVE);
                wp ^= bits;
            }
        }

        // Pawn simple promotions

        final long prbits = pawnsR7M << 8 & free;

        for (long d = prbits; d != 0L; d &= d - 1L) {
            final long toMask = d & -d;
            final int to = checker(toMask);
            final int from = to - 8;

            if ((toMask & pinMask(from)) != 0L) {
                legals[++last] = to << 10 | from |
                    WHITE_QUEEN << 6 | PROMOTE_MOVE;;
            }
        }
    }


    /**
     * Generates underpromotion moves for the white player and stores them
     * on the global moves array.
     */
    private final void genUnderpromsWhite() {
        final long pawnsR7M = pawns & R7_MATCH;

        if (pawnsR7M == 0L)
            return;

        // Pawn capture & promote moves

        long ep = (pawnsR7M & CH_CLEAR) << 7 & enemy;

        for (int p = BLACK_QUEEN; ep != 0L && p >= BLACK_PAWN; p--) {
            final long bits = ep & bitboards[p];

            if (bits != 0L) {
                final int flags = p << 16 | PROMCAP_MOVE;
                for (int r = WHITE_KNIGHT; r > WHITE_QUEEN; r--)
                    unpackPawnMoves(-7, bits, r << 6 | flags);
                ep ^= bits;
            }
        }

        long wp = (pawnsR7M & CA_CLEAR) << 9 & enemy;

        for (int p = BLACK_QUEEN; wp != 0L && p >= BLACK_PAWN; p--) {
            final long bits = wp & bitboards[p];

            if (bits != 0L) {
                final int flags = p << 16 | PROMCAP_MOVE;
                for (int r = WHITE_KNIGHT; r > WHITE_QUEEN; r--)
                    unpackPawnMoves(-9, bits, r << 6 | flags);
                wp ^= bits;
            }
        }

        // Pawn simple promotions

        final long prbits = pawnsR7M << 8 & free;

        for (long d = prbits; d != 0L; d &= d - 1L) {
            final long toMask = d & -d;
            final int to = checker(toMask);
            final int from = to - 8;

            if ((toMask & pinMask(from)) != 0L) {
                final int flags = to << 10 |from | PROMOTE_MOVE;
                for (int r = WHITE_KNIGHT; r > WHITE_QUEEN; r--)
                    legals[++last] = r << 6 | flags;
            }
        }
    }


    /**
     * Generates capture moves for the white player and stores them on the
     * global moves array.
     */
    private final void genCapturesWhite() {
        // Pawn simple captures

        final long pawnsR7C = pawns & R7_CLEAR;

        if (pawnsR7C != 0L) {
            long ea = (pawnsR7C & CH_CLEAR) << 7 & enemy;

            for (int p = BLACK_QUEEN; ea != 0L && p >= BLACK_PAWN; p--) {
                final long bits = ea & bitboards[p];
                unpackPawnMoves(-7, bits, p << 16 | WP_CAPTURE);
                ea ^= bits;
            }

            long wa = (pawnsR7C & CA_CLEAR) << 9 & enemy;

            for (int p = BLACK_QUEEN; wa != 0L && p >= BLACK_PAWN; p--) {
                final long bits = wa & bitboards[p];
                unpackPawnMoves(-9, bits, p << 16 | WP_CAPTURE);
                wa ^= bits;
            }
        }

        // Knight captures

        for (long o = knights; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long attacks = knightAttacks(from) & pinMask(from);

            if (attacks == 0L) continue;

            for (int p = BLACK_QUEEN; p >= BLACK_PAWN; p--) {
                final long bits = attacks & bitboards[p];
                unpackMoves(from, bits, p << 16 | WN_CAPTURE);
            }
        }

        // Bishop captures

        for (long o = bishops; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long attacks = bishopAttacks(from) & pinMask(from);

            if (attacks == 0L) continue;

            for (int p = BLACK_QUEEN; p >= BLACK_PAWN; p--) {
                final long bits = attacks & bitboards[p];
                unpackMoves(from, bits, p << 16 | WB_CAPTURE);
            }
        }

        // Rook captures

        for (long o = rooks; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long attacks = rookAttacks(from) & pinMask(from);

            if (attacks == 0L) continue;

            for (int p = BLACK_QUEEN; p >= BLACK_PAWN; p--) {
                final long bits = attacks & bitboards[p];
                unpackMoves(from, bits, p << 16 | WR_CAPTURE);
            }
        }

        // Queen captures

        for (long o = queens; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long attacks = queenAttacks(from) & pinMask(from);

            if (attacks == 0L) continue;

            for (int p = BLACK_QUEEN; p >= BLACK_PAWN; p--) {
                final long bits = attacks & bitboards[p];
                unpackMoves(from, bits, p << 16 | WQ_CAPTURE);
            }
        }

        // King captures

        final long attacksK = kattacks & ~_kattacks;

        if (attacksK != 0L) {
            for (int p = BLACK_QUEEN; p >= BLACK_PAWN; p--) {
                final long bits = attacksK & bitboards[p];
                unpackKingMoves(bits, p << 16 | WK_CAPTURE);
            }
        }

        // En-passant captures

        if (passant == NULL_PASSANT || pawns == 0L)
            return;

        final int to = passant;
        final long toMask = 1L << to;
        final long captMask = toMask >>> 8;

        occupied ^= captMask;
        _pawns ^= captMask;

        if (((toMask & CA_CLEAR) >> 7 & pawns) != 0L) {
            final int from = to - 7;
            if ((toMask & pinMask(from)) != 0L)
                legals[++last] = to << 10 | from | WP_PASSANT;
        }

        if (((toMask & CH_CLEAR) >> 9 & pawns) != 0L) {
            final int from = to - 9;
            if ((toMask & pinMask(from)) != 0L)
                legals[++last] = to << 10 | from | WP_PASSANT;
        }

        _pawns ^= captMask;
        occupied ^= captMask;
    }


    /**
     * Generates castles and non-capturing moves for the white player and
     * stores them on the global moves array.
     */
    private final void genOtherMovesWhite() {
        // Castling moves

        if ((castle & 0x3) != 0 && !inCheck()) {
            if ((castle & 0x1) != 0 && (occupied & 0x60L) == 0 &&
                !isAttacked(F1) && !isAttacked(G1)) {
                legals[++last] = WS_CASTLE;
            }

            if ((castle & 0x2) != 0 && (occupied & 0x0EL) == 0 &&
                !isAttacked(C1) && !isAttacked(D1)) {
                legals[++last] = WL_CASTLE;
            }
        }

        // Knight simple moves

        for (long o = knights; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long bits = knightAttacks(from) & pinMask(from);
            unpackMoves(from, free & bits, WN_SIMPLE);
        }

        // Bishop simple moves

        for (long o = bishops; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long bits = bishopAttacks(from) & pinMask(from);
            unpackMoves(from, free & bits, WB_SIMPLE);
        }

        // Rook simple moves

        for (long o = rooks; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long bits = rookAttacks(from) & pinMask(from);
            unpackMoves(from, free & bits, WR_SIMPLE);
        }

        // Queen simple moves

        for (long o = queens; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long bits = queenAttacks(from) & pinMask(from);
            unpackMoves(from, free & bits, WQ_SIMPLE);
        }

        // King simple moves

        final long attacksK = kattacks & ~_kattacks;
        unpackKingMoves(free & attacksK, WK_SIMPLE);

        if (pawns == 0L)
            return;

        // Pawn single advances

        final long sabits = free & ((pawns & R7_CLEAR) << 8);
        unpackPawnMoves(-8, sabits, WP_SIMPLE);

        // Pawn double advances

        final long dabits = free & ((sabits & R3_MATCH) << 8);
        unpackPawnMoves(-16, dabits, WP_SIMPLE);
    }


    /**
     * Generates legal King moves for the white player and stores them on
     * the global moves array.
     */
    private final void genKingMovesWhite() {
        final long attacksK = kattacks & ~_kattacks;

        if (attacksK == 0L)
            return;

        for (int p = BLACK_QUEEN; p >= BLACK_PAWN; p--) {
            final long bits = attacksK & bitboards[p];
            unpackKingMoves(bits, p << 16 | WK_CAPTURE);
        }

        unpackKingMoves(free & attacksK, WK_SIMPLE);
    }


    /**
     * Generates promotion moves for the black player and stores them on the
     * global moves array.
     */
    private final void genPromotionsBlack() {
        final long pawnsR2M = pawns & R2_MATCH;

        if (pawnsR2M == 0L)
            return;

        // Pawn capture & promote moves

        long ep = (pawnsR2M & CA_CLEAR) >>> 7 & enemy;

        for (int p = WHITE_QUEEN; ep != 0L && p <= WHITE_PAWN; p++) {
            final long bits = ep & bitboards[p];

            if (bits != 0L) {
                unpackPawnMoves(7, bits, p << 16 |
                    BLACK_QUEEN << 6 | PROMCAP_MOVE);
                ep ^= bits;
            }
        }

        long wp = (pawnsR2M & CH_CLEAR) >>> 9 & enemy;

        for (int p = WHITE_QUEEN; wp != 0L && p <= WHITE_PAWN; p++) {
            final long bits = wp & bitboards[p];

            if (bits != 0L) {
                unpackPawnMoves(9, bits, p << 16 |
                    BLACK_QUEEN << 6 | PROMCAP_MOVE);
                wp ^= bits;
            }
        }

        // Pawn simple promotions

        final long prbits = pawnsR2M >>> 8 & free;

        for (long d = prbits; d != 0L; d &= d - 1L) {
            final long toMask = d & -d;
            final int to = checker(toMask);
            final int from = to + 8;

            if ((toMask & pinMask(from)) != 0L) {
                legals[++last] = to << 10 | from |
                    BLACK_QUEEN << 6 | PROMOTE_MOVE;
            }
        }
    }


    /**
     * Generates underpromotion moves for the black player and stores them
     * on the global moves array.
     */
    private final void genUnderpromsBlack() {
        final long pawnsR2M = pawns & R2_MATCH;

        if (pawnsR2M == 0L)
            return;

        // Pawn capture & promote moves

        long ep = (pawnsR2M & CA_CLEAR) >>> 7 & enemy;

        for (int p = WHITE_QUEEN; ep != 0L && p <= WHITE_PAWN; p++) {
            final long bits = ep & bitboards[p];

            if (bits != 0L) {
                final int flags = p << 16 | PROMCAP_MOVE;
                for (int r = BLACK_KNIGHT; r < BLACK_QUEEN; r++)
                    unpackPawnMoves(7, bits, r << 6 | flags);
                ep ^= bits;
            }
        }

        long wp = (pawnsR2M & CH_CLEAR) >>> 9 & enemy;

        for (int p = WHITE_QUEEN; wp != 0L && p <= WHITE_PAWN; p++) {
            final long bits = wp & bitboards[p];

            if (bits != 0L) {
                final int flags = p << 16 | PROMCAP_MOVE;
                for (int r = BLACK_KNIGHT; r < BLACK_QUEEN; r++)
                    unpackPawnMoves(9, bits, r << 6 | flags);
                wp ^= bits;
            }
        }

        // Pawn promotions

        final long prbits = pawnsR2M >>> 8 & free;

        for (long d = prbits; d != 0L; d &= d - 1L) {
            final long toMask = d & -d;
            final int to = checker(toMask);
            final int from = to + 8;

            if ((toMask & pinMask(from)) != 0L) {
                final int flags = to << 10 | from | PROMOTE_MOVE;
                for (int r = BLACK_KNIGHT; r < BLACK_QUEEN; r++)
                    legals[++last] = r << 6 | flags;
            }
        }
    }


    /**
     * Generates capture moves for the black player and stores them on the
     * global moves array.
     */
    private final void genCapturesBlack() {
        // Pawn simple captures

        final long pawnsR2C = pawns & R2_CLEAR;

        if (pawnsR2C != 0L) {
            long ea = (pawnsR2C & CA_CLEAR) >>> 7 & enemy;

            for (int p = WHITE_QUEEN; ea != 0L && p <= WHITE_PAWN; p++) {
                final long bits = ea & bitboards[p];
                unpackPawnMoves(7, bits, p << 16 | BP_CAPTURE);
                ea ^= bits;
            }

            long wa = (pawnsR2C & CH_CLEAR) >>> 9 & enemy;

            for (int p = WHITE_QUEEN; wa != 0L && p <= WHITE_PAWN; p++) {
                final long bits = wa & bitboards[p];
                unpackPawnMoves(9, bits, p << 16 | BP_CAPTURE);
                wa ^= bits;
            }
        }

        // Knight captures

        for (long o = knights; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long attacks = knightAttacks(from) & pinMask(from);

            if (attacks == 0L) continue;

            for (int p = WHITE_QUEEN; p <= WHITE_PAWN; p++) {
                final long bits = attacks & bitboards[p];
                unpackMoves(from, bits, p << 16 | BN_CAPTURE);
            }
        }

        // Bishop captures

        for (long o = bishops; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long attacks = bishopAttacks(from) & pinMask(from);

            if (attacks == 0L) continue;

            for (int p = WHITE_QUEEN; p <= WHITE_PAWN; p++) {
                final long bits = attacks & bitboards[p];
                unpackMoves(from, bits, p << 16 | BB_CAPTURE);
            }
        }

        // Rook captures

        for (long o = rooks; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long attacks = rookAttacks(from) & pinMask(from);

            if (attacks == 0L) continue;

            for (int p = WHITE_QUEEN; p <= WHITE_PAWN; p++) {
                final long bits = attacks & bitboards[p];
                unpackMoves(from, bits, p << 16 | BR_CAPTURE);
            }
        }

        // Queen captures

        for (long o = queens; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long attacks = queenAttacks(from) & pinMask(from);

            if (attacks == 0L) continue;

            for (int p = WHITE_QUEEN; p <= WHITE_PAWN; p++) {
                final long bits = attacks & bitboards[p];
                unpackMoves(from, bits, p << 16 | BQ_CAPTURE);
            }
        }

        // King captures

        final long attacksK = kattacks & ~_kattacks;

        if (attacksK != 0L) {
            for (int p = WHITE_QUEEN; p <= WHITE_PAWN; p++) {
                final long bits = attacksK & bitboards[p];
                unpackKingMoves(bits, p << 16 | BK_CAPTURE);
            }
        }

        // En-passant captures

        if (passant == NULL_PASSANT || pawns == 0L)
            return;

        final int to = passant;
        final long toMask = 1L << to;
        final long captMask = toMask << 8;

        occupied ^= captMask;
        _pawns ^= captMask;

        if (((toMask & CH_CLEAR) << 7 & pawns) != 0L) {
            final int from = to + 7;
            if ((toMask & pinMask(from)) != 0L)
                legals[++last] = to << 10 | from | BP_PASSANT;
        }

        if (((toMask & CA_CLEAR) << 9 & pawns) != 0L) {
            final int from = to + 9;
            if ((toMask & pinMask(from)) != 0L)
                legals[++last] = to << 10 | from | BP_PASSANT;
        }

        _pawns ^= captMask;
        occupied ^= captMask;
    }


    /**
     * Generates castles and non-capturing moves for the black player and
     * stores them on the global moves array.
     */
    private final void genOtherMovesBlack() {
        // Castling moves

        if ((castle & 0xC) != 0 && !inCheck()) {
            if ((castle & 0x4) != 0 && (occupied & (0x60L << 56)) == 0 &&
                !isAttacked(F8) && !isAttacked(G8)) {
                legals[++last] = BS_CASTLE;
            }

            if ((castle & 0x8) != 0 && (occupied & (0x0EL << 56)) == 0 &&
                !isAttacked(C8) && !isAttacked(D8)) {
                legals[++last] = BL_CASTLE;
            }
        }

        // Knight simple moves

        for (long o = knights; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long bits = knightAttacks(from) & pinMask(from);
            unpackMoves(from, free & bits, BN_SIMPLE);
        }

        // Bishop simple moves

        for (long o = bishops; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long bits = bishopAttacks(from) & pinMask(from);
            unpackMoves(from, free & bits, BB_SIMPLE);
        }

        // Rook simple moves

        for (long o = rooks; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long bits = rookAttacks(from) & pinMask(from);
            unpackMoves(from, free & bits, BR_SIMPLE);
        }

        // Queen simple moves

        for (long o = queens; o != 0L; o &= o - 1L) {
            final int from = checker(o & -o);
            final long bits = queenAttacks(from) & pinMask(from);
            unpackMoves(from, free & bits, BQ_SIMPLE);
        }

        // King simple moves

        final long attacksK = kattacks & ~_kattacks;
        unpackKingMoves(free & attacksK, BK_SIMPLE);

        if (pawns == 0L)
            return;

        // Pawn single advances

        final long sabits = free & ((pawns & R2_CLEAR) >>> 8);
        unpackPawnMoves(8, sabits, BP_SIMPLE);

        // Pawn double advances

        final long dabits = free & ((sabits & R6_MATCH) >>> 8);
        unpackPawnMoves(16, dabits, BP_SIMPLE);
    }


    /**
     * Generates legal King moves for the black player and stores them on
     * the global moves array.
     */
    private final void genKingMovesBlack() {
        final long attacksK = kattacks & ~_kattacks;

        if (attacksK == 0L)
            return;

        for (int p = WHITE_QUEEN; p <= WHITE_PAWN; p++) {
            final long bits = attacksK & bitboards[p];
            unpackKingMoves(bits, p << 16 | BK_CAPTURE);
        }

        unpackKingMoves(free & attacksK, BK_SIMPLE);
    }


    /**
     * Unpacks a list of moves into the legal moves array placeholder.
     *
     * @param from      Source checker
     * @param bits      Destination checkers bitboard
     * @param flags     Move description flags
     */
    private final void unpackMoves(int from, long bits, int flags) {
        flags |= from;

        for (long d = bits; d != 0L; d &= d - 1L) {
            final int to = checker(d & -d);
            legals[++last] = to << 10 | flags;
        }
    }


    /**
     * Unpacks a list of King moves into the legal moves array placeholder.
     * This method produces legal moves by checking that the destination
     * square is not attacked.
     *
     * @param bits      Destination checkers bitboard
     * @param flags     Move description flags
     */
    private final void unpackKingMoves(long bits, int flags) {
        flags |= target;

        for (long d = bits; d != 0L; d &= d - 1L) {
            final int to = checker(d & -d);

            if (isLegalKingMove(to))
                legals[++last] = to << 10 | flags;
        }
    }


    /**
     * Unpacks a list of pawn moves into the legal moves array placeholder.
     * This method produces legal moves by checking that the pawn is not
     * absolutely pinned.
     *
     * @param disp      Source checker displacement
     * @param bits      Destination checkers bitboard
     * @param flags     Move description flags
     */
    private final void unpackPawnMoves(int disp, long bits, int flags) {
        for (long d = bits; d != 0L; d &= d - 1L) {
            final long toMask = d & -d;
            final int to = checker(toMask);
            final int from = to + disp;

            if ((pinMask(from) & toMask) != 0L)
                legals[++last] = to << 10 | from | flags;
        }
    }


    /**
     * Updates bitboard variables for the pieces.
     */
    private final void updateBitboards() {
        if (turn == WHITE) {
            loyal =    bitboards[WHITE_PIECES];
            enemy =    bitboards[BLACK_PIECES];
            king =     bitboards[WHITE_KING];
            pawns =    bitboards[WHITE_PAWN];
            knights =  bitboards[WHITE_KNIGHT];
            queens =   bitboards[WHITE_QUEEN];
            bishops =  bitboards[WHITE_BISHOP];
            rooks =    bitboards[WHITE_ROOK];
            _king =    bitboards[BLACK_KING];
            _pawns =   bitboards[BLACK_PAWN];
            _knights = bitboards[BLACK_KNIGHT];
            _queens =  bitboards[BLACK_QUEEN];
            _bishops = bitboards[BLACK_BISHOP];
            _rooks =   bitboards[BLACK_ROOK];
        } else {
            loyal =    bitboards[BLACK_PIECES];
            enemy =    bitboards[WHITE_PIECES];
            king =     bitboards[BLACK_KING];
            pawns =    bitboards[BLACK_PAWN];
            knights =  bitboards[BLACK_KNIGHT];
            queens =   bitboards[BLACK_QUEEN];
            bishops =  bitboards[BLACK_BISHOP];
            rooks =    bitboards[BLACK_ROOK];
            _king =    bitboards[WHITE_KING];
            _pawns =   bitboards[WHITE_PAWN];
            _knights = bitboards[WHITE_KNIGHT];
            _queens =  bitboards[WHITE_QUEEN];
            _bishops = bitboards[WHITE_BISHOP];
            _rooks =   bitboards[WHITE_ROOK];
        }

        occupied = bitboards[ALL_PIECES];
        free = ~occupied;

        target = checker(king);
        kattacks = kingAttacks(target);

        _target = checker(_king);
        _kattacks = kingAttacks(_target);

        _slidersB = _queens | _bishops;
        _slidersR = _queens | _rooks;
    }


    /**
     * Makes a simple move on the bitboards representation and updates
     * the current position information.
     *
     * @param move  Move identifier
     */
    private final void makeSimpleMove(int move) {
        final int piece = move >> 6 & 0xF;
        final int from = move & 0x3F;
        final int to = move >> 10;
        final long mask = 1L << to | 1L << from;

        bitboards[ALL_PIECES] ^= mask;
        bitboards[color]      ^= mask;
        bitboards[piece]      ^= mask;

        hash ^= ZOBRIST[piece][from];
        hash ^= ZOBRIST[piece][to];

        updateCastle(move);
        clearPassant();
    }


    /**
     * Unmakes a simple move on the bitboards representation.
     *
     * @param move  Move identifier
     */
    private final void unmakeSimpleMove(int move) {
        final int piece = move >> 6 & 0xF;
        final int from = move & 0x3F;
        final int to = move >> 10;
        final long mask = 1L << to | 1L << from;

        bitboards[ALL_PIECES] ^= mask;
        bitboards[color]      ^= mask;
        bitboards[piece]      ^= mask;
    }


    /**
     * Makes a simple pawn move on the bitboards representation and updates
     * the current position information.
     *
     * @param move  Move identifier
     */
    private final void makePawnMove(int move) {
        final int piece = color ^ 0x8;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final long mask = 1L << to | 1L << from;

        bitboards[ALL_PIECES] ^= mask;
        bitboards[color]      ^= mask;
        bitboards[piece]      ^= mask;

        hash ^= ZOBRIST[piece][from];
        hash ^= ZOBRIST[piece][to];

        updatePassant(move);
        this.advance = this.index;
    }


    /**
     * Unmakes a simple pawn move on the bitboards representation.
     *
     * @param move  Move identifier
     */
    private final void unmakePawnMove(int move) {
        final int piece = color ^ 0x8;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final long mask = 1L << to | 1L << from;

        bitboards[ALL_PIECES] ^= mask;
        bitboards[color]      ^= mask;
        bitboards[piece]      ^= mask;
    }


    /**
     * Makes a capturing move on the bitboards representation and updates
     * the current position information.
     *
     * @param move  Move identifier
     */
    private final void makeCapture(int move) {
        final int capture = move >> 16 & 0xF;
        final int piece = move >> 6 & 0xF;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final long toMask = 1L << to;
        final long fromMask = 1L << from;
        final long mask = toMask | fromMask;

        bitboards[ALL_PIECES]  ^= fromMask;
        bitboards[color ^ 0x3] ^= toMask;
        bitboards[color]       ^= mask;
        bitboards[piece]       ^= mask;
        bitboards[capture]     ^= toMask;

        hash ^= ZOBRIST[piece][from];
        hash ^= ZOBRIST[piece][to];
        hash ^= ZOBRIST[capture][to];

        updateCastle(move);
        clearPassant();
        this.advance = this.index;
    }


    /**
     * Unmakes a capturing move on the bitboards representation.
     *
     * @param move  Move identifier
     */
    private final void unmakeCapture(int move) {
        final int capture = move >> 16 & 0xF;
        final int piece = move >> 6 & 0xF;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final long toMask = 1L << to;
        final long fromMask = 1L << from;
        final long mask = toMask | fromMask;

        bitboards[ALL_PIECES]  ^= fromMask;
        bitboards[color ^ 0x3] ^= toMask;
        bitboards[color]       ^= mask;
        bitboards[piece]       ^= mask;
        bitboards[capture]     ^= toMask;
    }


    /**
     * Makes a pawn promotion on the bitboards representation and updates
     * the current position information.
     *
     * @param move  Move identifier
     */
    private final void makePromotion(int move) {
        final int piece = color ^ 0x8;
        final int promo = move >> 6 & 0xF;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final long toMask = 1L << to;
        final long fromMask = 1L << from;
        final long mask = toMask | fromMask;

        bitboards[ALL_PIECES]  ^= mask;
        bitboards[color]       ^= mask;
        bitboards[piece]       ^= fromMask;
        bitboards[promo]       ^= toMask;

        hash ^= ZOBRIST[piece][from];
        hash ^= ZOBRIST[promo][to];

        clearPassant();
        this.advance = this.index;
    }


    /**
     * Unmakes a pawn promotion on the bitboards representation.
     *
     * @param move  Move identifier
     */
    private final void unmakePromotion(int move) {
        final int piece = color ^ 0x8;
        final int promo = move >> 6 & 0xF;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final long toMask = 1L << to;
        final long fromMask = 1L << from;
        final long mask = toMask | fromMask;

        bitboards[ALL_PIECES]  ^= mask;
        bitboards[color]       ^= mask;
        bitboards[piece]       ^= fromMask;
        bitboards[promo]       ^= toMask;
    }


    /**
     * Makes a capture move that leads to a pawn promotion on the bitboards
     * representation and updates the current position information.
     *
     * @param move  Move identifier
     */
    private final void makePromcap(int move) {
        final int piece = color ^ 0x8;
        final int capture = move >> 16 & 0xF;
        final int promo = move >> 6 & 0xF;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final long toMask = 1L << to;
        final long fromMask = 1L << from;

        bitboards[ALL_PIECES]  ^= fromMask;
        bitboards[color ^ 0x3] ^= toMask;
        bitboards[color]       ^= toMask | fromMask;
        bitboards[piece]       ^= fromMask;
        bitboards[promo]       ^= toMask;
        bitboards[capture]     ^= toMask;

        hash ^= ZOBRIST[piece][from];
        hash ^= ZOBRIST[promo][to];
        hash ^= ZOBRIST[capture][to];

        updateCastle(move);
        clearPassant();
        this.advance = this.index;
    }


    /**
     * Unmakes a capture move that leads to a pawn promotion on the
     * bitboards representation.
     *
     * @param move  Move identifier
     */
    private final void unmakePromcap(int move) {
        final int piece = color ^ 0x8;
        final int capture = move >> 16 & 0xF;
        final int promo = move >> 6 & 0xF;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final long toMask = 1L << to;
        final long fromMask = 1L << from;

        bitboards[ALL_PIECES]  ^= fromMask;
        bitboards[color ^ 0x3] ^= toMask;
        bitboards[color]       ^= toMask | fromMask;
        bitboards[piece]       ^= fromMask;
        bitboards[promo]       ^= toMask;
        bitboards[capture]     ^= toMask;
    }


    /**
     * Makes an en passant move on the bitboards representation and updates
     * the current position information.
     *
     * @param move  Move identifier
     */
    private final void makePassant(int move) {
        final int piece = color ^ 0x8;
        final int capture = color ^ 0xB;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final int passant = to ^ 0x8;
        final long captMask = 1L << passant;
        final long mask = 1L << to | 1L << from;

        bitboards[ALL_PIECES]  ^= captMask | mask;
        bitboards[color ^ 0x3] ^= captMask;
        bitboards[color]       ^= mask;
        bitboards[piece]       ^= mask;
        bitboards[capture]     ^= captMask;

        hash ^= ZOBRIST[piece][from];
        hash ^= ZOBRIST[piece][to];
        hash ^= ZOBRIST[capture][passant];

        clearPassant();
        this.advance = this.index;
    }


    /**
     * Unmakes an en passant move on the bitboards representation.
     *
     * @param move  Move identifier
     */
    private final void unmakePassant(int move) {
        final int piece = color ^ 0x8;
        final int capture = color ^ 0xB;
        final int from = move & 0x3F;
        final int to = move >> 10 & 0x3F;
        final int passant = to ^ 0x8;
        final long captMask = 1L << passant;
        final long mask = 1L << to | 1L << from;

        bitboards[ALL_PIECES]  ^= captMask | mask;
        bitboards[color ^ 0x3] ^= captMask;
        bitboards[color]       ^= mask;
        bitboards[piece]       ^= mask;
        bitboards[capture]     ^= captMask;
    }


    /**
     * Makes a castling move on the bitboards representation and updates
     * the current position information.
     *
     * @param move  Move identifier
     */
    private final void makeCastling(int move) {
        switch (move) {
            case BS_CASTLE:
                bitboards[ALL_PIECES]   ^= 0xF000000000000000L;
                bitboards[BLACK_PIECES] ^= 0xF000000000000000L;
                bitboards[BLACK_KING]   ^= 0x5000000000000000L;
                bitboards[BLACK_ROOK]   ^= 0xA000000000000000L;
                hash                    ^= BSC_ZOBRIST;
                break;
            case WS_CASTLE:
                bitboards[ALL_PIECES]   ^= 0x00000000000000F0L;
                bitboards[WHITE_PIECES] ^= 0x00000000000000F0L;
                bitboards[WHITE_KING]   ^= 0x0000000000000050L;
                bitboards[WHITE_ROOK]   ^= 0x00000000000000A0L;
                hash                    ^= WSC_ZOBRIST;
                break;
            case BL_CASTLE:
                bitboards[ALL_PIECES]   ^= 0x1D00000000000000L;
                bitboards[BLACK_PIECES] ^= 0x1D00000000000000L;
                bitboards[BLACK_KING]   ^= 0x1400000000000000L;
                bitboards[BLACK_ROOK]   ^= 0x0900000000000000L;
                hash                    ^= BLC_ZOBRIST;
                break;
            default: // White long castling
                bitboards[ALL_PIECES]   ^= 0x000000000000001DL;
                bitboards[WHITE_PIECES] ^= 0x000000000000001DL;
                bitboards[WHITE_KING]   ^= 0x0000000000000014L;
                bitboards[WHITE_ROOK]   ^= 0x0000000000000009L;
                hash                    ^= WLC_ZOBRIST;
        }

        clearPassant();
        clearCastle();
    }


    /**
     * Unmakes a castling move on the bitboards representation.
     *
     * @param move  Move identifier
     */
    private final void unmakeCastling(int move) {
        switch (move) {
            case BS_CASTLE:
                bitboards[ALL_PIECES]   ^= 0xF000000000000000L;
                bitboards[BLACK_PIECES] ^= 0xF000000000000000L;
                bitboards[BLACK_KING]   ^= 0x5000000000000000L;
                bitboards[BLACK_ROOK]   ^= 0xA000000000000000L;
                break;
            case WS_CASTLE:
                bitboards[ALL_PIECES]   ^= 0x00000000000000F0L;
                bitboards[WHITE_PIECES] ^= 0x00000000000000F0L;
                bitboards[WHITE_KING]   ^= 0x0000000000000050L;
                bitboards[WHITE_ROOK]   ^= 0x00000000000000A0L;
                break;
            case BL_CASTLE:
                bitboards[ALL_PIECES]   ^= 0x1D00000000000000L;
                bitboards[BLACK_PIECES] ^= 0x1D00000000000000L;
                bitboards[BLACK_KING]   ^= 0x1400000000000000L;
                bitboards[BLACK_ROOK]   ^= 0x0900000000000000L;
                break;
            default: // White long castling
                bitboards[ALL_PIECES]   ^= 0x000000000000001DL;
                bitboards[WHITE_PIECES] ^= 0x000000000000001DL;
                bitboards[WHITE_KING]   ^= 0x0000000000000014L;
                bitboards[WHITE_ROOK]   ^= 0x0000000000000009L;
        }
    }


    /**
     * Updates the current en-passant checker.
     *
     * @param move  Move identifier
     */
    private final void updatePassant(int move) {
        final int bits = move & 0xE3F8;

        hash ^= passant;
        passant = (bits == 0x6148 || bits == 0x81B0) ?
            (byte) (move & 0x3F ^ 0x18) : NULL_PASSANT;
        hash ^= passant;
    }


    /**
     * Updates the current position castling rights.
     *
     * @param move  Move identifier
     */
    private final void updateCastle(int move) {
        byte ocastle = castle;

        if (turn == WHITE) {
            if ((castle & 0x3) != 0) {
                switch (move & 0x0003FF) {
                    case (WHITE_ROOK << 6 | A1):
                        castle &= 0xD;
                        break;
                    case (WHITE_ROOK << 6 | H1):
                        castle &= 0xE;
                        break;
                    case (WHITE_KING << 6 | E1):
                        castle &= 0xC;
                        break;
                }
            }

            if ((castle & 0xC) != 0 && isCapture(move)) {
                switch (move & 0x0FFC00) {
                    case (BLACK_ROOK << 16 | A8 << 10):
                        castle &= 0x7;
                        break;
                    case (BLACK_ROOK << 16 | H8 << 10):
                        castle &= 0xB;
                        break;
                }
            }
        } else {
            if ((castle & 0xC) != 0) {
                switch (move & 0x0003FF) {
                    case (BLACK_ROOK << 6 | A8):
                        castle &= 0x7;
                        break;
                    case (BLACK_ROOK << 6 | H8):
                        castle &= 0xB;
                        break;
                    case (BLACK_KING << 6 | E8):
                        castle &= 0x3;
                        break;
                }
            }

            if ((castle & 0x3) != 0 && isCapture(move)) {
                switch (move & 0x0FFC00) {
                    case (WHITE_ROOK << 16 | A1 << 10):
                        castle &= 0xD;
                        break;
                    case (WHITE_ROOK << 16 | H1 << 10):
                        castle &= 0xE;
                        break;
                }
            }
        }

        if (castle != ocastle) {
            hash ^= CR_ZOBRIST * ocastle;
            hash ^= CR_ZOBRIST * castle;
        }
    }


    /**
     * Clears the current en-passant checker.
     */
    private final void clearPassant() {
        if (passant != NULL_PASSANT) {
            hash ^= passant ^ NULL_PASSANT;
            passant = NULL_PASSANT;
        }
    }


    /**
     * Clears the castling rights of the current player.
     */
    private final void clearCastle() {
        if (castle != NULL_CASTLE) {
            hash ^= CR_ZOBRIST * castle;
            castle &= (turn == WHITE) ? 0xC : 0x3;
            hash ^= CR_ZOBRIST * castle;
        }
    }


    /**
     * Returns whether the last performed move was a capture.
     *
     * @return  {@code true} if the last move captured a piece
     */
    public final boolean wasCapture() {
        return (move & CAPTURE_MOVE) != 0;
    }


    /**
     * Returns if the move identifies a capture move.
     *
     * @param move  Move identifier
     * @return      {@code true} if it's a capturing move
     */
    public final boolean isCapture(int move) {
        return (move & CAPTURE_MOVE) != 0;
    }


    /**
     * Returns if the move identifies an en-passant capture.
     *
     * @param move  Move identifier
     * @return      {@code true} if it's an en-passant move
     */
    public final boolean isPassant(int move) {
        return (move & 0xF00000) == PASSANT_MOVE;
    }


    /**
     * Returns if the move identifies a castling.
     *
     * @param move  Move identifier
     * @return      {@code true} if it's a castling move
     */
    public final boolean isCastling(int move) {
        return (move & 0xF00000) == CASTLE_MOVE;
    }


    /**
     * Returns if the move identifies a pawn promotion.
     *
     * @param move  Move identifier
     * @return      {@code true} if it's a promotion
     */
    public final boolean isPromotion(int move) {
        return (move & 0x800000) != 0;
    }


    /**
     * Returns the attacks for a King on the given checker.
     *
     * @param checker   Square identifier
     * @return          Bitboard
     */
    public final long kingAttacks(int checker) {
        return KING_ATTACKS[checker];
    }


    /**
     * Returns the attacks for a Knight on the given checker.
     *
     * @param checker   Square identifier
     * @return          Bitboard
     */
    public final long knightAttacks(int checker) {
        return KNIGHT_ATTACKS[checker];
    }


    /**
     * Returns the attacks for a Pawn on the given checker.
     *
     * @param checker   Square identifier
     * @param color     Pawn color
     * @return          Bitboard
     */
    public final long pawnAttacks(int checker, int color) {
        final long bit = 1L << checker;

        return color == WHITE_PIECES ?
            (bit & CA_CLEAR) << 9  | (bit & CH_CLEAR) << 7 :
            (bit & CA_CLEAR) >>> 7 | (bit & CH_CLEAR) >>> 9;
    }


    /**
     * Returns the attacks for a Bishop on the given checker.
     *
     * @param checker   Square identifier
     * @return          Bitboard
     */
    public final long bishopAttacks(int checker) {
        final long[] attacks = BISHOP_ATTACKS[checker];
        final long magic = attacks[0];
        final long mask = attacks[1];
        final int hash = (int) ((occupied & mask) * magic >>> 55);
        final int index = BISHOP_INDEX[checker][hash];

        return attacks[index];
    }


    /**
     * Returns the attacks for a Rook on the given checker.
     *
     * @param checker   Square identifier
     * @return          Bitboard
     */
    public final long rookAttacks(int checker) {
        final long[] attacks = ROOK_ATTACKS[checker];
        final long magic = attacks[0];
        final long mask = attacks[1];
        final int hash = (int) ((occupied & mask) * magic >>> 52);
        final int index = 0xFF & ROOK_INDEX[checker][hash];

        return attacks[index];
    }


    /**
     * Returns the attacks for a Queen on the given checker.
     *
     * @param checker   Square identifier
     * @return          Bitboard
     */
    public final long queenAttacks(int checker) {
        return bishopAttacks(checker) | rookAttacks(checker);
    }


    /**
     * Returns a mask of possible moves for a pinned piece or an
     * {@code ALL_MATCH} mask if the piece is not pinned. If the King is
     * in check the mask will contain only valid check evasions.
     *
     * @param checker   Square identifier
     * @return          Bitboard mask
     */
    public final long pinMask(int checker) {
        boolean is_bslider = false;

        // Pieces on the sides cannot be pinned

        final int row = checker & 0x07;
        final int col = checker & 0x38;

        if ((target & 0x07) == row) {
            if (col == 0 || col == 0x38)
                return evasion;
        } else if ((target & 0x38) == col) {
            if (row == 0 || row == 0x07)
                return evasion;
        } else {
            if (row == 0 || row == 0x07 || col == 0 || col == 0x38)
                return evasion;
            is_bslider = true;
        }

        // Return a mask if the piece is truly pinned

        final long mask = PIN_ATTACKS[target][checker];

        if (mask != 0L) {
            if (is_bslider) {
                if ((mask & _slidersB) != 0L) {
                    final long attacks = bishopAttacks(checker);
                    if ((attacks & king) != 0L &&
                        (mask & attacks & _slidersB) != 0L) {
                        return mask & evasion;
                    }
                }
            } else if ((mask & _slidersR) != 0L) {
                final long attacks = rookAttacks(checker);
                if ((attacks & king) != 0L &&
                    (mask & attacks & _slidersR) != 0L) {
                    return mask & evasion;
                }
            }
        }

        return evasion;
    }


    /**
     * Returns a mask for the possible check evasions or {@code ALL_MATCH}
     * if the King is not in check. The returned evasion mask does not include
     * King moves.
     *
     * @return  Bitboard mask
     */
    public final long evasionMask() {
        long mask = ALL_CLEAR;

        // Disjoint Pawn and Knight attacks

        final long attacksP = (_pawns != 0L) ?
            pawnAttacks(target, color) : 0L;

        if ((attacksP & _pawns) != 0L) {
            mask = attacksP & _pawns;

            if (passant != NULL_PASSANT)
                 mask |= 1L << passant;
        } else if (_knights != 0L) {
            mask = _knights & knightAttacks(target);
        }

        // Single and double attacks with a non-sliding piece

        if (mask != 0L) {
            if (_slidersB != 0L && (
                _slidersB & bishopAttacks(target)) != 0L)
                return ALL_CLEAR;

            if (_slidersR != 0L && (
                _slidersR & rookAttacks(target)) != 0L)
                return ALL_CLEAR;

            return mask;
        }

        // Single and double sliding piece attacks

        final long attacksB = (_slidersB != 0L) ?
            bishopAttacks(target) : 0L;

        final long attacksR = (_slidersR != 0L) ?
            rookAttacks(target) : 0L;

        if ((attacksB & _slidersB) != 0L) {
            if ((attacksR & _slidersR) != 0L)
                return ALL_CLEAR;

            final int checker = checker(attacksB & _slidersB);
            final long pins = PIN_ATTACKS[target][checker];

            return attacksB & pins;
        } else if ((attacksR & _slidersR) != 0L) {
            final int checker = checker(attacksR & _slidersR);
            final long pins = PIN_ATTACKS[target][checker];

            return attacksR & pins;
        }

        return ALL_MATCH;
    }


    /**
     * Converts a binary representation of a board checker to an integer.
     * That is, given a power of two, returns its exponent.
     *
     * @param bit   A power of two representing a checker
     * @return      Checker number
     */
    public static final int checker(long bit) {
        return CHECKER_INDEX[(int) (bit * DEBRUIJN >>> 58)];
    }


    /**
     * {@inheritDoc}
     */
    public void ensureCapacity(int minCapacity) {
        // Make sure size doesn't exceed max capacity

        if (minCapacity <= capacity)
            return;

        if (minCapacity > MAX_CAPACITY) {
            throw new IllegalStateException(
                "Requested capacity is above the maximum");
        } else {
            capacity = Math.min(
                MAX_CAPACITY,
                Math.max(
                    minCapacity,
                    capacity + CAPACITY_INCREMENT
                )
            );
        }

        // Copy data into new arrays

        int[] cmoves = new int[capacity];
        int[] cadvances = new int[capacity];
        int[] clegals = new int[capacity << 8];
        int[] cnexts = new int[capacity];
        int[] clasts = new int[capacity];
        byte[] ccastles = new byte[capacity];
        byte[] cpassants = new byte[capacity];
        long[] chashes = new long[capacity];
        long[] cevasions = new long[capacity];

        System.arraycopy(moves, 0, cmoves, 0, index + 1);
        System.arraycopy(advances, 0, cadvances, 0, index + 1);
        System.arraycopy(legals, 0, clegals, 0, (index + 1) << 8);
        System.arraycopy(nexts, 0, cnexts, 0, index + 1);
        System.arraycopy(legals, 0, clegals, 0, index + 1);
        System.arraycopy(castles, 0, ccastles, 0, index + 1);
        System.arraycopy(passants, 0, cpassants, 0, index + 1);
        System.arraycopy(hashes, 0, chashes, 0, index + 1);
        System.arraycopy(evasions, 0, cevasions, 0, index + 1);

        moves = cmoves;
        advances = cadvances;
        legals = clegals;
        nexts = cnexts;
        lasts = clasts;
        castles = ccastles;
        passants = cpassants;
        hashes = chashes;
        evasions = cevasions;

        System.gc();
    }


    /* Precomputed arrays and constants */

    /** Index identifiers for the checkers */
    private static final int[] CHECKER_INDEX = {
        A1, B1, A7, C1, B8, B7, E4, D1,
        F8, C8, C7, C6, G5, F4, B3, E1,
        G8, H7, D8, E5, F7, D7, D6, G3,
        F6, H5, B5, G4, A4, C3, E2, F1,
        H8, H6, A8, D4, E8, B6, F5, A3,
        G7, D5, E7, F3, E6, A5, H3, D2,
        G6, C4, A6, H2, C5, E3, H4, C2,
        B4, G2, D3, B2, F2, A2, H1, G1
    };

    /** King attacks by checker */
    private static final long[] KING_ATTACKS;

    /** Knight attacks by checker */
    private static final long[] KNIGHT_ATTACKS;

    /** Bishop attacks by checker and blockers */
    private static final long[][] BISHOP_ATTACKS;

    /** Rook attacks by checker and blockers */
    private static final long[][] ROOK_ATTACKS;

    /** Pinned attacks by King and piece checkers */
    private static final long[][] PIN_ATTACKS;

    /** Bishop attacks index (unsigned) */
    private static final byte[][] BISHOP_INDEX;

    /** Rook attacks index (unsigned) */
    private static final byte[][] ROOK_INDEX;

    /** Zobrist hashing randoms */
    private static final long[][] ZOBRIST;

    /** White short castling Zobrist hash */
    private static final long WSC_ZOBRIST;

    /** White long castling Zobrist hash */
    private static final long WLC_ZOBRIST;

    /** Black short castling Zobrist hash */
    private static final long BSC_ZOBRIST;

    /** Black long castling Zobrist hash */
    private static final long BLC_ZOBRIST;

    /** Castling rights multiplier */
    private static final long CR_ZOBRIST;

    /* Initialize constants */

    static {
        // Read precomputed arrays from disk

        String path = null;
        FileInputStream fis = null;
        ObjectInputStream ois = null;

        try {
            path = ChessGame.class.getResource("/chess.bin").getFile();
            fis = new FileInputStream(path);
            ois = new ObjectInputStream(fis);

            KING_ATTACKS = (long[]) ois.readObject();
            KNIGHT_ATTACKS = (long[]) ois.readObject();
            BISHOP_ATTACKS = (long[][]) ois.readObject();
            ROOK_ATTACKS = (long[][]) ois.readObject();
            PIN_ATTACKS = (long[][]) ois.readObject();
            BISHOP_INDEX = (byte[][]) ois.readObject();
            ROOK_INDEX = (byte[][]) ois.readObject();
            ZOBRIST = (long[][]) ois.readObject();

            ois.close();
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }

        // Zobrist constants for castlings

        CR_ZOBRIST  = 0X78EB9B5A5A499F1L;

        BSC_ZOBRIST = ZOBRIST[BLACK_KING][E8] ^ ZOBRIST[BLACK_KING][G8] ^
                      ZOBRIST[BLACK_ROOK][H8] ^ ZOBRIST[BLACK_ROOK][F8];

        WSC_ZOBRIST = ZOBRIST[WHITE_KING][E1] ^ ZOBRIST[WHITE_KING][G1] ^
                      ZOBRIST[WHITE_ROOK][H1] ^ ZOBRIST[WHITE_ROOK][F1];

        BLC_ZOBRIST = ZOBRIST[BLACK_KING][E8] ^ ZOBRIST[BLACK_KING][C8] ^
                      ZOBRIST[BLACK_ROOK][A8] ^ ZOBRIST[BLACK_ROOK][D8];

        WLC_ZOBRIST = ZOBRIST[WHITE_KING][E1] ^ ZOBRIST[WHITE_KING][C1] ^
                      ZOBRIST[WHITE_ROOK][A1] ^ ZOBRIST[WHITE_ROOK][D1];
    }
}
