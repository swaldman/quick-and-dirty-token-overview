pragma solidity ^0.4.18;

import "https://github.com/OpenZeppelin/openzeppelin-solidity/blob/d51e38758e1d985661534534d5c61e27bece5042/contracts/math/SafeMath.sol";

contract SaferSimpleToken {

  using SafeMath for uint;

  mapping ( address => uint ) public balanceOf;
  uint public totalSupply;

  constructor ( uint initialSupply ) public {
    balanceOf[ msg.sender ] = balanceOf[ msg.sender ].add( initialSupply );
    totalSupply = initialSupply;
  }
  
  function transfer(address to, uint tokens) public returns (bool success) {
    require( balanceOf[ msg.sender ] >= tokens );
    balanceOf[ msg.sender ] = balanceOf[ msg.sender ].sub( tokens );
    balanceOf[ to ] = balanceOf[ to ].add( tokens );
    emit Transfer( msg.sender, to, tokens );
    success = true;      
  }

  event Transfer(address indexed from, address indexed to, uint tokens);
}
