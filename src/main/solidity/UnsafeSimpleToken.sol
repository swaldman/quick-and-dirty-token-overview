pragma solidity ^0.4.22;

contract UnsafeSimpleToken {
  mapping ( address => uint ) public balanceOf;
  uint public totalSupply;

  constructor ( uint initialSupply ) public {
    balanceOf[ msg.sender ] += initialSupply;
    totalSupply = initialSupply;
  }

  function transfer(address to, uint tokens) public returns (bool success) {
    require( balanceOf[ msg.sender ] >= tokens );
    balanceOf[ msg.sender ] -= tokens;   
    balanceOf[to] += tokens;
    emit Transfer( msg.sender, to, tokens );
    success = true;      
  }

  event Transfer(address indexed from, address indexed to, uint tokens);
}
