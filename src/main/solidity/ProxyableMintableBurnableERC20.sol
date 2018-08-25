pragma solidity ^0.4.24;

import "./MintableBurnableERC20.sol";

/*
 * But it might be better to collapse elaborate inheritance hierarchies!
 */ 
contract ProxyableMintableBurnableERC20 is MintableBurnableERC20 {

  /*
   *  This reproduces work that occurs in a parent class constructor.
   *  Ideally, the constructor would be written in terms of this method to avoid code duplication
   *  but that would mean copying and modifying the external library we are importing.
   *
   *  (In production, it may be best to just do that.)
   *
   *  We need to be able to reproduce the constructor logic in a function call in order to
   *  support (potentially upgradable) proxies.
   *        
   *  ** NOTE require() CALL ENSURING THIS CAN ONLY BE CALLED ON AN UNINITIALIZED CONTRACT **
   *  ** MILLIONS OF DOLLARS HAVE BEEN LOST FOR LACK OF SUCH A CHECK                       **
   *
   */
  function initialize( address _owner ) public {
    require(owner == 0);
    owner = _owner;
  }
}
