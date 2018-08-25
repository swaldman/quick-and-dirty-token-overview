pragma solidity ^0.4.22;

contract HelloWorld {
  function sayHello( string to )
    public
    pure
    returns (string greeting, string recipient) {
      greeting = "Hello!";
      recipient = to;
  }
}
