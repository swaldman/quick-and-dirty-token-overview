pragma solidity ^0.4.24;

import "https://github.com/OpenZeppelin/openzeppelin-solidity/blob/7618b91d9cc51c4b6dfeca209af8daf1992abddc/contracts/token/ERC20/MintableToken.sol";
import "https://github.com/OpenZeppelin/openzeppelin-solidity/blob/7618b91d9cc51c4b6dfeca209af8daf1992abddc/contracts/token/ERC20/BurnableToken.sol";

/*
 * But it might be better to collapse elaborate inheritance hierarchies!
 */ 
contract MintableBurnableERC20 is MintableToken, BurnableToken {
}
