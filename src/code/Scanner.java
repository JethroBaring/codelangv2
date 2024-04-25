package code;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Scanner {
  private final String source;
  private final List<Token> tokens = new ArrayList<>();
  private int start = 0;
  private int current = 0;
  private int line = 1;
  private static final Map<String, TokenType> keywords;

  static {
    keywords = new HashMap<>();
    keywords.put("BEGIN", TokenType.BEGIN);
    keywords.put("END", TokenType.END);
    keywords.put("CODE", TokenType.CODE);
    keywords.put("DISPLAY", TokenType.DISPLAY);
    keywords.put("SCAN", TokenType.SCAN);
    keywords.put("null", TokenType.NULL);
    keywords.put("SCAN", TokenType.SCAN);
    keywords.put("STRING", TokenType.STRING);
    keywords.put("CHAR", TokenType.CHAR);
    keywords.put("INT", TokenType.INT);
    keywords.put("FLOAT", TokenType.FLOAT);
    keywords.put("BOOL", TokenType.BOOL);
    keywords.put("IF", TokenType.IF);
    keywords.put("ELSE", TokenType.ELSE);
    keywords.put("WHILE", TokenType.WHILE);
    keywords.put("AND", TokenType.AND);
    keywords.put("OR", TokenType.OR);
    keywords.put("NOT", TokenType.NOT);
    keywords.put("FN", TokenType.FUNCTION);
    keywords.put("IMMUT", TokenType.IMMUTABLE);
    keywords.put("RETURN", TokenType.RETURN);
  }

  Scanner(String source) {
    this.source = source;
  }

  List<Token> scanTokens() {
    while (!isAtEnd()) {
      start = current;
      scanToken();
    }
    tokens.add(new Token(TokenType.EOF, "", null, line));
    return tokens;
  }

  private boolean isAtEnd() {
    return current >= source.length();
  }

  private void scanToken() {
    char c = advance();
    switch (c) {
      case '(':
        addToken(TokenType.LEFT_PARENTHESIS, null);
        break;
      case ')':
        addToken(TokenType.RIGHT_PARENTHESIS, null);
        break;
      case '[':
        if (peekNext() == ']') {
          char escapedCharacter = advance();
          start += 1;
          addToken(TokenType.CHAR_LITERAL, escapedCharacter);
          advance();
        } else {
          addToken(TokenType.RIGHT_BRACKET);
        }
        break;
      case ']':
        addToken(TokenType.RIGHT_BRACKET);
        break;
      case '{':
        addToken(TokenType.LEFT_BRACE);
        break;
      case '}':
        addToken(TokenType.RIGHT_BRACE);
        break;
      case ',':
        addToken(TokenType.COMMA);
        break;
      case ':':
        addToken(TokenType.COLON);
        break;
      case '+':
        addToken(TokenType.PLUS);
        break;
      case '-':
        addToken(TokenType.MINUS);
        break;
      case '*':
        addToken(TokenType.STAR);
        break;
      case '/':
        addToken(TokenType.SLASH);
        break;
      case '&':
        addToken(TokenType.AMPERSAND);
        break;
      case '$':
        addToken(TokenType.DOLLAR_SIGN, '\n');
        break;
      case '%':
        addToken(TokenType.MODULO);
        break;
      case '=':
        if (match('=')) {
          addToken(TokenType.EQUAL_EQUAL);
        } else {
          addToken(TokenType.EQUAL);
        }
        break;
      case '>':
        if (match('=')) {
          addToken(TokenType.GREATER_THAN_EQUAL);
        } else {
          addToken(TokenType.GREATER_THAN);
        }
        break;
      case '<':
        if (match('=')) {
          addToken(TokenType.LESS_THAN_EQUAL);
        } else if (match('>')) {
          addToken(TokenType.NOT_EQUAL);
        } else {
          addToken(TokenType.LESS_THAN);
        }
        break;
      case ';':
        addToken(TokenType.SEMICOLON);
        break;
      case '#':
        while (!isAtNewLine() && !isAtEnd())
          advance();
        break;
      case '\0':
      case ' ':
      case '\t':
      case '\r':
        break;
      case '\n':
        line++;
        break;
      case '"':
        string();
        break;
      case '\'':
        if (peekNext() == '\'') {
          char escapedCharacter = advance();
          start += 1;
          addToken(TokenType.CHAR_LITERAL, escapedCharacter);
          advance();
        }
        break;
      default:
        if (isDigit(c)) {
          number();
        } else if (isAlpha(c)) {
          identifier();
        } else {
          Code.error(line, current, "Unexpected character.");
        }
        break;
    }
  }

  private boolean match(char expected) {
    if (isAtEnd())
      return false;
    if (source.charAt(current) != expected)
      return false;
    current++;
    return true;
  }

  private char peek() {
    if (isAtEnd() || isAtNewLine())
      return '\0';
    return source.charAt(current);
  }

  private char peekNext() {
    if (isAtEnd() || isAtNewLine())
      return '\0';
    return source.charAt(current + 1);
  }

  private boolean isAtNewLine() {
    if (current < source.length()) {
      return source.charAt(current) == '\n';
    }
    return false;
  }

  private boolean isAlpha(char c) {
    return (c >= 'a' && c <= 'z') ||
        (c >= 'A' && c <= 'Z') ||
        c == '_';
  }

  private boolean isDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private boolean isFloat(String value) {
    if (value.contains(".")) {
      return true;
    }

    return false;
  }

  private boolean isAlphaNumeric(char c) {
    return isAlpha(c) || isDigit(c);
  }

  private char advance() {
    current++;
    return source.charAt(current - 1);
  }

  private void addToken(TokenType type) {
    addToken(type, null);
  }

  private void addToken(TokenType type, Object literal) {
    String text = source.substring(start, current);
    tokens.add(new Token(type, text, literal, line));
  }

  private void string() {
    while (peek() != '"' && !isAtNewLine()) {
      advance();
    }

    if (isAtNewLine()) {
      Code.error(line, current, "Unterminated string.");
      return;
    }

    advance();

    String value = source.substring(start + 1, current - 1);
    if (value.equals("TRUE")) {
      addToken(TokenType.TRUE_LITERAL, value);
    } else if (value.equals("FALSE")) {
      addToken(TokenType.FALSE_LITERAL, value);
    } else {
      addToken(TokenType.STRING_LITERAL, value);
    }
  }

  private void number() {
    while (isDigit(peek())) {
      advance();
    }

    if (peek() == '.' && isDigit(peekNext())) {
      advance();
      while (isDigit(peek())) {
        advance();
      }
    }

    if (isAlpha(peek())) {
      while (isAlphaNumeric(peek())) {
        advance();
      }
      Code.error(line, current, "Unexpected character found after a number.");
      return;
    }

    String value = source.substring(start, current);

    if (isFloat(value)) {
      addToken(TokenType.FLOAT_LITERAL, Double.parseDouble(value));
    } else {
      addToken(TokenType.INT_LITERAL, Integer.parseInt(value));
    }
  }

  private void identifier() {
    while (isAlphaNumeric(peek())) {
      advance();
    }

    String text = source.substring(start, current);
    TokenType type = keywords.get(text);
    if (type == null)
      type = TokenType.IDENTIFIER;
    addToken(type);
  }
}
