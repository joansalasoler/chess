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

import com.joansala.engine.Game;


/**
 *
 * @author    Joan Sala Soler
 * @version   1.0.0
 */
public final class Chess {

    public static final boolean DEBUG = true;

    /* Default values */

    /** Empty piece identifier */
    public static final int NULL_PIECE = 0x3F;

    /** Empty piece identifier */
    public static final byte NULL_PASSANT = 0x7F;

    /** No castling rights */
    public static final byte NULL_CASTLE = 0x00;

    /** Default castling rights */
    public static final byte START_CASTLE = 0x0F;

    /** Moves that can be made without pawn advances or captures */
    public static final byte HALF_MOVES = 100;

    /* Player identifiers */

    public static final int WHITE = Game.SOUTH;
    public static final int BLACK = Game.NORTH;

    /* Move types flags (promote, capture, castle, pawn) */

    public static final int PROMCAP_MOVE = 0xD00000;
    public static final int PROMOTE_MOVE = 0x900000;
    public static final int PASSANT_MOVE = 0x500000;
    public static final int CAPTURE_MOVE = 0x400000;
    public static final int CASTLE_MOVE  = 0x200000;
    public static final int PAWN_MOVE    = 0x100000;
    public static final int SIMPLE_MOVE  = 0x000000;

    /* Piece identifiers */

    public static final int WHITE_KING   =  0;
    public static final int WHITE_QUEEN  =  1;
    public static final int WHITE_ROOK   =  2;
    public static final int WHITE_BISHOP =  3;
    public static final int WHITE_KNIGHT =  4;
    public static final int WHITE_PAWN   =  5;

    public static final int BLACK_PAWN   =  6;
    public static final int BLACK_KNIGHT =  7;
    public static final int BLACK_BISHOP =  8;
    public static final int BLACK_ROOK   =  9;
    public static final int BLACK_QUEEN  = 10;
    public static final int BLACK_KING   = 11;

    public static final int ALL_PIECES   = 12;
    public static final int WHITE_PIECES = 13;
    public static final int BLACK_PIECES = 14;

    /* Checker identifiers */

    public static final int
        A1 =  0, B1 =  1, C1 =  2, D1 =  3,
        E1 =  4, F1 =  5, G1 =  6, H1 =  7,
        A2 =  8, B2 =  9, C2 = 10, D2 = 11,
        E2 = 12, F2 = 13, G2 = 14, H2 = 15,
        A3 = 16, B3 = 17, C3 = 18, D3 = 19,
        E3 = 20, F3 = 21, G3 = 22, H3 = 23,
        A4 = 24, B4 = 25, C4 = 26, D4 = 27,
        E4 = 28, F4 = 29, G4 = 30, H4 = 31,
        A5 = 32, B5 = 33, C5 = 34, D5 = 35,
        E5 = 36, F5 = 37, G5 = 38, H5 = 39,
        A6 = 40, B6 = 41, C6 = 42, D6 = 43,
        E6 = 44, F6 = 45, G6 = 46, H6 = 47,
        A7 = 48, B7 = 49, C7 = 50, D7 = 51,
        E7 = 52, F7 = 53, G7 = 54, H7 = 55,
        A8 = 56, B8 = 57, C8 = 58, D8 = 59,
        E8 = 60, F8 = 61, G8 = 62, H8 = 63;

    /* Castling moves */

    public static final int WS_CASTLE = CASTLE_MOVE | G1 << 10 | WHITE_KING << 6 | E1;
    public static final int WL_CASTLE = CASTLE_MOVE | C1 << 10 | WHITE_KING << 6 | E1;
    public static final int BS_CASTLE = CASTLE_MOVE | G8 << 10 | BLACK_KING << 6 | E8;
    public static final int BL_CASTLE = CASTLE_MOVE | C8 << 10 | BLACK_KING << 6 | E8;

    /* Predefined move flags */

    public static final int WP_SIMPLE =  WHITE_PAWN   << 6 | PAWN_MOVE;
    public static final int WP_PASSANT = WHITE_PAWN   << 6 | PASSANT_MOVE;
    public static final int WP_CAPTURE = WHITE_PAWN   << 6 | CAPTURE_MOVE;

    public static final int WN_SIMPLE =  WHITE_KNIGHT << 6 | SIMPLE_MOVE;
    public static final int WB_SIMPLE =  WHITE_BISHOP << 6 | SIMPLE_MOVE;
    public static final int WR_SIMPLE =  WHITE_ROOK   << 6 | SIMPLE_MOVE;
    public static final int WQ_SIMPLE =  WHITE_QUEEN  << 6 | SIMPLE_MOVE;
    public static final int WK_SIMPLE =  WHITE_KING   << 6 | SIMPLE_MOVE;

    public static final int BP_SIMPLE =  BLACK_PAWN   << 6 | PAWN_MOVE;
    public static final int BP_PASSANT = BLACK_PAWN   << 6 | PASSANT_MOVE;
    public static final int BP_CAPTURE = BLACK_PAWN   << 6 | CAPTURE_MOVE;

    public static final int BN_SIMPLE =  BLACK_KNIGHT << 6 | SIMPLE_MOVE;
    public static final int BB_SIMPLE =  BLACK_BISHOP << 6 | SIMPLE_MOVE;
    public static final int BR_SIMPLE =  BLACK_ROOK   << 6 | SIMPLE_MOVE;
    public static final int BQ_SIMPLE =  BLACK_QUEEN  << 6 | SIMPLE_MOVE;
    public static final int BK_SIMPLE =  BLACK_KING   << 6 | SIMPLE_MOVE;

    public static final int WN_CAPTURE = WHITE_KNIGHT << 6 | CAPTURE_MOVE;
    public static final int WB_CAPTURE = WHITE_BISHOP << 6 | CAPTURE_MOVE;
    public static final int WR_CAPTURE = WHITE_ROOK   << 6 | CAPTURE_MOVE;
    public static final int WQ_CAPTURE = WHITE_QUEEN  << 6 | CAPTURE_MOVE;
    public static final int WK_CAPTURE = WHITE_KING   << 6 | CAPTURE_MOVE;

    public static final int BN_CAPTURE = BLACK_KNIGHT << 6 | CAPTURE_MOVE;
    public static final int BB_CAPTURE = BLACK_BISHOP << 6 | CAPTURE_MOVE;
    public static final int BR_CAPTURE = BLACK_ROOK   << 6 | CAPTURE_MOVE;
    public static final int BQ_CAPTURE = BLACK_QUEEN  << 6 | CAPTURE_MOVE;
    public static final int BK_CAPTURE = BLACK_KING   << 6 | CAPTURE_MOVE;

    /* Board masks */

    public static final long ALL_MATCH =      0xFFFFFFFFFFFFFFFFL;
    public static final long ALL_CLEAR =      0x0000000000000000L;
    public static final long WHITE_CHECKERS = 0x55AA55AA55AA55AAL;
    public static final long BLACK_CHECKERS = 0xAA55AA55AA55AA55L;

    public static final long CA_CLEAR =       0x7F7F7F7F7F7F7F7FL;
    public static final long CH_CLEAR =       0xFEFEFEFEFEFEFEFEL;
    public static final long R2_CLEAR =       0xFFFFFFFFFFFF00FFL;
    public static final long R7_CLEAR =       0xFF00FFFFFFFFFFFFL;

    public static final long R2_MATCH =       0x000000000000FF00L;
    public static final long R3_MATCH =       0x0000000000FF0000L;
    public static final long R6_MATCH =       0x0000FF0000000000L;
    public static final long R7_MATCH =       0x00FF000000000000L;

    /* Move generation status */

    public static final byte GEN_KINGMOVES  = 0;
    public static final byte GEN_PROMOTIONS = 1;
    public static final byte GEN_CAPTURES   = 2;
    public static final byte GEN_OTHERMOVES = 3;
    public static final byte GEN_UNDERPROMS = 4;
    public static final byte GEN_FINALIZED  = 5;

    /** Default start bitboards */
    public static final long[] START_BITBOARDS = {
        0x0000000000000010L, //  0: White King
        0x0000000000000008L, //  1: White Queens
        0x0000000000000081L, //  2: White Rooks
        0x0000000000000024L, //  3: White Bishops
        0x0000000000000042L, //  4: White Knights
        0x000000000000FF00L, //  5: White Pawns
        0x00FF000000000000L, //  6: Black Pawns
        0x4200000000000000L, //  7: Black Knights
        0x2400000000000000L, //  8: Black Bishops
        0x8100000000000000L, //  9: Black Rooks
        0x0800000000000000L, // 10: Black Queens
        0x1000000000000000L, // 11: Black King
        0xFFFF00000000FFFFL, // 12: All pieces
        0x000000000000FFFFL, // 13: White pieces
        0xFFFF000000000000L  // 14: Black pieces
    };
}
