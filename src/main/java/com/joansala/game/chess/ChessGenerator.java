package com.joansala.game.chess;

/*
 * Aalina engine.
 * Copyright (C) 2021-2024 Joan Sala Soler <contact@joansala.com>
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

import com.joansala.game.chess.attacks.*;
import static com.joansala.engine.Game.*;
import static com.joansala.util.bits.Bits.*;
import static com.joansala.game.chess.Chess.*;


/**
 * Move generator for chess.
 */
public class ChessGenerator {

    /** First move generation cursor */
    public static final int UNGENERATED = 0;

    /** Default number of slots */
    private static final int DEFAULT_CAPACITY = 255;

    /** Maximum possible moves by a single stage */
    private static final int MAX_MOVES = 218;

    /** No move generation was performed */
    private static final int START_STAGE = -1;

    /** Generate king moves stage */
    private static final int KING_STAGE = 0;

    /** Move generation was completed */
    private static final int END_STAGE = 14;

    /** Current capacity */
    private int capacity = DEFAULT_CAPACITY;

    /** Stores moves and remnants for each slot */
    private Entry[] store = new Entry[DEFAULT_CAPACITY];

    /** Current player to move */
    private Player player;

    /** Current evasions mask */
    private long evasions;

    /** Current position bitboards */
    private long[] state;

    /** Moves generated (current slot) */
    private int[] moves = null;

    /** Next move index */
    private int index = 0;


    /**
     * Create a new move generator.
     */
    public ChessGenerator() {
        for (int i = 0; i < store.length; i++) {
            store[i] = new Entry();
        }
    }


    /**
     * Extracts the move generation stage from a cursor.
     *
     * @param cursor    Generation cursor containing stage information
     * @return          Stage number (0-14) indicating which type of
     *                  moves to generate
     */
    public int getStage(int cursor) {
        return (cursor >> 8) & 0x1F;
    }


    /**
     * Extracts the encoded move from a cursor.
     *
     * @param cursor    Generation cursor containing move information
     * @return          Encoded move value
     */
    public int getMove(int cursor) {
        return (cursor >> 14);
    }


    /**
     * Determines if a player's king is currently in check.
     *
     * @param state     Bitboards representing the current game state
     * @param player    Player whose king to check
     * @return          If the king is in check
     */
    public boolean isInCheck(long[] state, Player player) {
        final long evasions = computeEvasions(state, player);
        return evasions != FULL_BOARD;
    }


    /**
     * Checks if a player is in checkmate or stalemate by generating the
     * first set of moves and verifiying if the result is empty.
     *
     * @param slot      Storage slot to use for move generation
     * @param state     Bitboards representing the current game state
     * @param player    Player to check for available moves
     * @return          If no legal moves exist
     */
    public boolean cannotMove(int slot, long[] state, Player player) {
        final Entry entry = store[slot];
        generate(slot, UNGENERATED, state, player);
        return 0 == entry.length;
    }


    /**
     * Next move generation cursor for a slot.
     *
     * @param slot      Storage slot
     * @param cursor    Generation cursor
     * @return          Next generation cursor
     */
    public int nextCursor(int slot, int cursor) {
        final Entry entry = store[slot];

        int stage = getStage(cursor);
        int index = (cursor & 0xFF);
        int move = entry.moves[index];

        if (index == entry.length - 1) {
            stage = entry.nextStage;
            index = 0;
        } else {
            index++;
        }

        return (move << 14) | (stage << 8) | (index);
    }


    /**
     * Clear moves from the given slot.
     *
     * @param slot      Storage slot
     */
    public void clear(int slot) {
        store[slot].clear();
    }


    /**
     * Generate the set of moves required by a cursor and store them on
     * the given slot if they weren't already generated.
     *
     * @param slot      Storage slot
     * @param cursor    Current generation cursor
     * @param state     Position bitboards
     * @param player    Player to move
     */
    public void generate(int slot, int cursor, long[] state, Player player) {
        final Entry entry = store[slot];
        long evasions = entry.evasions;
        int stage = getStage(cursor);

        if (entry.isCurrentStage(stage)) {
            return;
        }

        if (entry.isStartStage()) {
            evasions = computeEvasions(state, player);
            entry.evasions = evasions;
        }

        this.index = 0;
        this.state = state;
        this.player = player;
        this.evasions = evasions;
        this.moves = entry.moves;
        entry.currentStage = stage;

        if (isKingInDoubleCheck()) {
            if (stage == KING_STAGE) {
                storeStageMoves(stage, evasions);
                stage = END_STAGE;
            }
        } else {
            for (int i = index; i == index && stage < END_STAGE; stage++) {
                storeStageMoves(stage, evasions);
            }
        }

        entry.length = index;
        entry.nextStage = stage;
        moves[index] = NULL_MOVE;
    }


    /**
     * Generates the set of moves for the given stage appending them
     * to the current slot if any legal moves are found.
     *
     * @param stage         Generation stage
     * @param evasions      Check evasions mask
     */
    private void storeStageMoves(int stage, long evasions) {
        final long friends = state[player.side];
        final long rivals = state[1 ^ player.side];
        final long taken = state[WHITE] | state[BLACK];
        final long bishops = state[BISHOP] & friends;
        final long knights = state[KNIGHT] & friends;
        final long queens = state[QUEEN] & friends;
        final long rooks = state[ROOK] & friends;
        final long pawns = state[PAWN] & friends;
        final long king = state[KING] & friends;
        final long flags = state[FLAGS];

        // If the king is in check generate only check evasions, otherwise
        // all the rival pieces can be captured except for the king.

        final long checkers = ~(friends | state[KING]);
        final long mask = isKingInCheck() ? evasions : checkers;

        switch (stage) {
            case  0: storeKingMoves(king, taken, checkers); break;
            case  1: storeCastlings(taken, flags); break;
            case  2: storePromotions(pawns, taken, rivals, mask); break;
            case  3: storePawnCaptures(pawns, taken, mask & rivals); break;
            case  4: storePieceMoves(KNIGHT, knights, taken, mask & rivals); break;
            case  5: storePieceMoves(BISHOP, bishops, taken, mask & rivals); break;
            case  6: storePieceMoves(ROOK, rooks, taken, mask & rivals); break;
            case  7: storePieceMoves(QUEEN, queens, taken, mask & rivals); break;
            case  8: storeEnPassants(pawns, taken, flags); break;
            case  9: storePieceMoves(KNIGHT, knights, taken, mask & ~rivals); break;
            case 10: storePieceMoves(BISHOP, bishops, taken, mask & ~rivals); break;
            case 11: storePieceMoves(ROOK, rooks, taken, mask & ~rivals); break;
            case 12: storePieceMoves(QUEEN, queens, taken, mask & ~rivals); break;
            case 13: storePawnMoves(pawns, taken, mask); break;
        }
    }


    /**
     * Generate the legal moves of the King.
     *
     * @param king      Player king bitboard
     * @param taken     Board of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storeKingMoves(long king, long taken, long mask) {
        final int from = first(king);
        long attacks = King.attacks(from) & mask;

        while (empty(attacks) == false) {
            int to = first(attacks);
            attacks ^= bit(to);

            if (isAttacked(to, taken ^ king) == false) {
                store(UNFLAGGED, KING, from, to);
            }
        }
    }


    /**
     * Generate the legal moves for a piece type.
     *
     * @param pieces    Player pieces bitboard
     * @param taken     Bitboard of occupied checkers
     * @param mask      Bitboard of allowed checkers
     * @param pieceType Piece type constant
     */
    private void storePieceMoves(int pieceType, long pieces, long taken, long mask) {
        while (empty(pieces) == false) {
            final int from = first(pieces);
            final long attacks = getAttacks(pieceType, from, taken);
            final long targets = attacks & mask & pins(from, taken);
            store(UNFLAGGED, pieceType, from, targets);
            pieces ^= bit(from);
        }
    }


    /**
     * Generate the legal castling moves.
     *
     * @param taken     Bitboard of occupied checkers
     * @param flags     Bitboard of rooks that can castle
     */
    private void storeCastlings(long taken, long flags) {
        if (isKingInCheck()) return;

        for (Castle castle : player.castlings) {
            final boolean hasRight = contains(flags, castle.flag);
            final boolean hasPath = empty(taken & castle.path);

            if (hasRight && hasPath && !areAttacked(castle.spots, taken)) {
                store(castle.move);
            }
        }
    }


    /**
     * Generate the legal en-passant pawn captures.
     *
     * @param pawns     Player pawns bitboard
     * @param taken     Bitboard of occupied checkers
     * @param flags     En-passant bitboard
     */
    private void storeEnPassants(long pawns, long taken, long flags) {
        final long mask = shift(evasions, player.sense);
        final long target = (flags & ~CASTLE_MASK);

        if (!isKingInCheck() || contains(mask, target)) {
            final int to = first(target);
            long attackers = pawns & Pawn.attacks(to, 64 ^ player.sense);

            while (empty(attackers) == false) {
                final int from = first(attackers);
                final long pins = pins(from, taken & ~bit(to ^ 0x8));
                attackers ^= bit(from);

                if (contains(pins, target)) {
                    store(ENPASSANT, PAWN, from, to);
                }
            }
        }
    }


    /**
     * Generate the legal capture moves of pawns.
     *
     * @param pawns     Player pawns bitboard
     * @param taken     Bitboard of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storePawnCaptures(long pawns, long taken, long mask) {
        if (empty(pawns = pawns & ~player.seventh) == false) {
            final int sense = player.sense;
            final int oneRow = player.turn << 3;

            long lefts = Pawn.lefts(pawns, sense) & mask;
            long rights = Pawn.rights(pawns, sense) & mask;

            processPawnTargets(lefts, oneRow - 1, taken);
            processPawnTargets(rights, oneRow + 1, taken);
        }
    }


    /**
     * Generate the legal non-capturing moves of pawns.
     *
     * @param pawns     Player pawns bitboard
     * @param taken     Bitboard of occupied checkers
     * @param mask      Bitboard of allowed checkers
     */
    private void storePawnMoves(long pawns, long taken, long mask) {
        if (empty(pawns = pawns & ~player.seventh) == false) {
            final int sense = player.sense;
            final int oneRow = player.turn << 3;
            final int twoRows = player.turn << 4;
            final long bases = pawns & player.base;

            long doubles = Pawn.doubles(bases, taken, sense) & mask;
            long singles = Pawn.singles(pawns, taken, sense) & mask;

            processPawnTargets(doubles, twoRows, taken);
            processPawnTargets(singles, oneRow, taken);
        }
    }


    /**
     * Generate the legal pawn promotion moves.
     *
     * @param pawns     Player pawns bitboard
     * @param taken     Bitboard of occupied checkers
     * @param rivals    Bitboard of rival pieces
     * @param mask      Bitboard of allowed checkers
     */
    private void storePromotions(long pawns, long taken, long rivals, long mask) {
        if (empty(pawns = pawns & player.seventh) == false) {
            final int sense = player.sense;

            long lefts = Pawn.lefts(pawns, sense) & rivals & mask;
            long rights = Pawn.rights(pawns, sense) & rivals & mask;
            long singles = Pawn.singles(pawns, taken, sense) & mask;

            processPromotionTargets(lefts, 1, taken);
            processPromotionTargets(rights, -1, taken);
            processPromotionTargets(singles, 0, taken);
        }
    }


    /**
     * Processes pawn promotion moves and stores them.
     *
     * @param targets   Bitboard of promotion destination squares
     * @param offset    File offset to calculate source square
     * @param taken     Bitboard of occupied squares
     */
    private void processPromotionTargets(long targets, int offset, long taken) {
        while (empty(targets) == false) {
            final int to = first(targets);
            final int from = (to ^ 0x8) + offset;
            final long pins = pins(from, taken);
            targets ^= bit(to);

            if (contains(pins, bit(to))) {
                store(PROMOTION, QUEEN, from, to);
                store(PROMOTION, KNIGHT, from, to);
                store(PROMOTION, BISHOP, from, to);
                store(PROMOTION, ROOK, from, to);
            }
        }
    }


    /**
     * Processes regular pawn moves and stores them.
     *
     * @param targets   Bitboard of pawn destination squares
     * @param rowOffset Row offset to calculate source square
     * @param taken     Bitboard of occupied squares
     */
    private void processPawnTargets(long targets, int rowOffset, long taken) {
        while (empty(targets) == false) {
            final int to = first(targets);
            final int from = to - rowOffset;
            final long pins = pins(from, taken);
            targets ^= bit(to);

            if (contains(pins, bit(to))) {
                store(UNFLAGGED, PAWN, from, to);
            }
        }
    }


    /**
     * Computes the attack bitboard for a specific piece type.
     *
     * @param pieceType Type of piece (KNIGHT, BISHOP, ROOK, or QUEEN)
     * @param from      Square index where the piece is located
     * @param taken     Bitboard of all occupied squares
     * @return          Bitboard of squares the piece can attack
     * @throws IllegalArgumentException if pieceType is invalid
     */
    private long getAttacks(int pieceType, int from, long taken) {
        switch (pieceType) {
            case KNIGHT: return Knight.attacks(from);
            case BISHOP: return Bishop.attacks(from, taken);
            case ROOK: return Rook.attacks(from, taken);
            case QUEEN: return Queen.attacks(from, taken);
            default: throw new IllegalArgumentException("Invalid piece");
        }
    }


    /**
     * Checks if the current player's king is in check.
     *
     * @return      If king is in check
     */
    private boolean isKingInCheck() {
        return evasions != FULL_BOARD;
    }


    /**
     * Checks if the current player's king is in double check.
     *
     * @return      If king is attacked by two pieces
     */
    private boolean isKingInDoubleCheck() {
        return evasions == EMPTY_BOARD;
    }


    /**
     * Determines if a square is under attack by any opponent piece.
     *
     * @param checker   Square index to check for attacks
     * @param taken     Bitboard of all occupied squares
     * @return          If the square is attacked by any opponent piece
     */
    private boolean isAttacked(int checker, long taken) {
        final long rivals = state[1 ^ player.side];
        final long rooks = (state[QUEEN] | state[ROOK]) & rivals;
        final long bishops = (state[QUEEN] | state[BISHOP]) & rivals;
        final long knights = state[KNIGHT] & rivals;
        final long pawns = state[PAWN] & rivals;
        final long king = state[KING] & rivals;

        return (
            contains(pawns, Pawn.attacks(checker, player.sense)) ||
            contains(bishops, Bishop.attacks(checker, taken)) ||
            contains(rooks, Rook.attacks(checker, taken)) ||
            contains(knights, Knight.attacks(checker)) ||
            contains(king, King.attacks(checker))
        );
    }


    /**
     * Checks if either of two squares is under attack by opponent pieces.
     * Used for castling validation to ensure the king doesn't pass
     * through check.
     *
     * @param spots     Array of exactly two square indices to check
     * @param taken     Bitboard of all occupied squares
     * @return          If either square is attacked by opponent pieces
     */
    private boolean areAttacked(int[] spots, long taken) {
        return isAttacked(spots[0], taken) || isAttacked(spots[1], taken);
    }


    /**
     * Computes a bitboard mask of legal destination squares for pieces
     * when the king is in check.
     *
     * The returned bitboard contains the checkers from which the attacking
     * pieces can be captured or their attacks blocked. This does not include
     * en-passant captures. Thus, the following statements hold true for the
     * returned evasions mask:
     *
     * a) If the returned bitboard is full the king is not in check.
     * b) If the returned bitboard is empty the king is in double check.
     * c) A piece cannot move to a square not contained on the evasions.
     *    Except fot the en-passant capture if any.
     *
     * @param state     Bitboards representing the current game state
     * @param player    Player whose king might be in check
     * @return          Bitboard mask of legal destination squares
     */
    private long computeEvasions(long[] state, Player player) {
        final long rivals = state[1 ^ player.side];
        final long taken = state[WHITE] | state[BLACK];
        final long rooks = (state[QUEEN] | state[ROOK]) & rivals;
        final long bishops = (state[QUEEN] | state[BISHOP]) & rivals;
        final long knights = state[KNIGHT] & rivals;
        final long pawns = state[PAWN] & rivals;
        final int target = first(state[KING] & ~rivals);

        // Checkers from which to capture a knight or pawn attacker

        long evasions = pawns & Pawn.attacks(target, player.sense);
        evasions |= knights & Knight.attacks(target);

        // If a knight/pawn is attacking return the evasions, or an
        // empty board if the king is in double check

        final long rookAttacks = Rook.attacks(target, taken);
        final long bishopAttacks = Bishop.attacks(target, taken);
        final int bishopCount = count(bishops & bishopAttacks);
        final int rookCount = count(rooks & rookAttacks);
        final int sliderCount = bishopCount + rookCount;

        if (empty(evasions) == false) {
            return (sliderCount > 0) ? EMPTY_BOARD : evasions;
        }

        // If a sliding piece is attacking return a ray of checkers from
        // which to stop the check, or an empty board if in double check

        if (sliderCount > 1) {
            return EMPTY_BOARD;
        } else if (bishopCount > 0) {
            final int to = first(bishopAttacks & bishops);
            return bishopAttacks & Ray.ray(target, to);
        } else if (rookCount > 0) {
            final int to = first(rookAttacks & rooks);
            return rookAttacks & Ray.ray(target, to);
        }

        return FULL_BOARD;
    }


    /**
     * Calculates movement restrictions for a potentially pinned piece.
     * A piece is pinned if moving it would expose the king to check.
     *
     * @param from      Square index where the piece is located
     * @param taken     Bitboard of all occupied squares
     * @return          FULL_BOARD if piece is not pinned, otherwise a ray
     *                  bitboard representing the only legal movement
     *                  direction for the pinned piece
     */
    private long pins(int from, long taken) {
        final long rivals = state[1 ^ player.side];
        final long king = state[KING] & ~rivals;
        final int target = first(king);
        final long ray = Ray.ray(target, from);

        if (empty(ray)) {
            return FULL_BOARD;
        }

        final long diff = from ^ target;

        if (empty((diff & 7) * (diff & 56))) { // In same row or column
            final long attackers = (state[QUEEN] | state[ROOK]) & rivals;

            if (contains(attackers, ray)) {
                final long attacks = Rook.attacks(from, taken);
                return pinsRay(attackers, ray, king, attacks);
            }
        } else {
            final long attackers = (state[QUEEN] | state[BISHOP]) & rivals;

            if (contains(attackers, ray)) {
                final long attacks = Bishop.attacks(from, taken);
                return pinsRay(attackers, ray, king, attacks);
            }
        }

        return FULL_BOARD;
    }


    /**
     * Checks if there's an attacking piece on the ray and if the piece's
     * attacks include the king.
     *
     * @param attackers     Bitboard of potential attacking pieces
     * @param ray           Ray bitboard from king to the piece being checked
     * @param king          Bitboard containing only the king position
     * @param attacks       Attack bitboard from the piece being checked for pins
     * @return              Ray bitboard if piece is pinned, FULL_BOARD otherwise
     */
    private long pinsRay(long attackers, long ray, long king, long attacks) {
        final boolean hasKing = contains(attacks, king);
        final boolean hasAttacker = contains(ray & attacks, attackers);
        return (hasKing && hasAttacker) ? ray : FULL_BOARD;
    }


    /**
     * Unpacks a bitboard of target squares into individual encoded moves.
     * Each set bit in targets becomes a separate move.
     *
     * @param move      Base move encoding
     * @param targets   Bitboard of destination squares
     */
    private void unpack(int move, long targets) {
        while (empty(targets) == false) {
            int to = first(targets);
            targets ^= bit(to);
            store(move | to);
        }
    }


    /**
     * Stores multiple moves with the same source and piece type to the
     * current slot. Only processes if targets bitboard is not empty.
     *
     * @param flag      Move type flag
     * @param piece     Piece type constant
     * @param from      Source square index
     * @param targets   Bitboard of destination squares
     */
    private void store(int flag, int piece, int from, long targets) {
        if (empty(targets) == false) {
            unpack(flag | (piece << 12) | (from << 6), targets);
        }
    }


    /**
     * Stores a single move to the current slot.
     *
     * @param flag      Move type flag
     * @param piece     Piece type constant
     * @param from      Source square index
     * @param to        Destination square index
     */
    private void store(int flag, int piece, int from, int to) {
        store(flag | (piece << 12) | (from << 6) | to);
    }


    /**
     * Stores a fully encoded move to the current slot.
     *
     * @param move      Encoding containing all move information
     */
    private void store(int move) {
        moves[index++] = move;
    }


    /**
     * Increases the storage capacity of this generator to accommodate
     * more slots.
     *
     * @param size      New number of slots
     */
    public void ensureCapacity(int size) {
        if (size > capacity) {
            store = Arrays.copyOf(store, size);

            for (int slot = capacity; slot < size; slot++) {
                store[slot] = new Entry();
            }

            capacity = size;
            System.gc();
        }
    }


    /**
     * An entry on the generated moves store.
     */
    private class Entry {

        /** Number of moves generated */
        int length = 0;

        /** Moves generated on current stage */
        int[] moves = new int[MAX_MOVES];

        /** Next generation stage */
        int nextStage = KING_STAGE;

        /** Current generation stage */
        int currentStage = START_STAGE;

        /** Bitboard of check evasions */
        long evasions = FULL_BOARD;


        /**
         * Checks if moves for a stage have already been generated.
         *
         * @param stage     Stage number to check
         * @return          If stage is within the current generation range
         */
        boolean isCurrentStage(int stage) {
            return stage >= currentStage && stage < nextStage;
        }


        /**
         * Checks if this entry is in its initial state.
         *
         * @return  If no move generation has been performed
         */
        boolean isStartStage() {
            return currentStage == START_STAGE;
        }


        /**
         * Resets this entry to its initial state.
         */
        void clear() {
            currentStage = START_STAGE;
            nextStage = KING_STAGE;
        }
    }
}
