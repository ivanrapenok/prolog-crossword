% Dictionary:
word(cart). word(castle). word(tear). 

cross(Word1, LetterNumber1, Word2, LetterNumber2) :-
    sub_string(Word1, LetterNumber1, 1, _, X),
    sub_string(Word2, LetterNumber2, 1, _, X).

word_with_length(Word, Length) :-
    word(Word),
    string_length(Word, Length).

% To run: crossword(W0, W1, W2).
crossword(W0, W1, W2) :-
    % Words lengths
    word_with_length(W0, 4),
    word_with_length(W1, 6),
    word_with_length(W2, 4),

    % All words are different
    append([], [W0, W1, W2], List),
    sort(List, Sorted),
    length(Sorted, Len),
    length(List, Len),

    % Words crosses
    cross(W1, 1, W0, 1),
    cross(W1, 3, W2, 0).

