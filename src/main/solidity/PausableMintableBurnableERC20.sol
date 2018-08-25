/*
 * NOTE: This example of upgradabiity is actually a bit DANGEROUS as
 *       the existence of the paused field does change the storage representation
 *       of the contract.
 * 
 *       However, since we are just appending a fields whose default value should be
 *       zero, it does work correctly. 
 * 
 *       But this is a fragile technique! You always have to be careful that, if the
 *       new implementation has storage at all different from the old implementation,
 *       the new implementation is compatible, either by default or via some call made
 *       in the same transaction via upgradeAndCall that properly initializes any new 
 *       state in the upgrade.
 */ 

pragma solidity ^0.4.24;

import "./ProxyableMintableBurnableERC20.sol";

// inspired by https://github.com/OpenZeppelin/openzeppelin-solidity/blob/master/contracts/lifecycle/Pausable.sol

/*
 * But it might be better to collapse these elaborate inheritance hierarchies!
 */ 
contract PausableMintableBurnableERC20 is ProxyableMintableBurnableERC20 {

  event Pause();
  event Unpause();

  bool public paused = false;

  modifier ifNotPaused() {
    require(!paused, "The contract has been paused!");
    _;
  }

  modifier ifPaused() {
    require(paused);
    _;
  }

  function pause()
    onlyOwner
    public {
      if ( !paused ) {
        paused = true;
	emit Pause();
      }
  }

  function unpause()
    onlyOwner
    public {
      if ( paused ) {
        paused = false;
	emit Unpause();
      }
  }

  function transfer(address to, uint tokens)
    public
    ifNotPaused
    returns (bool success) {
      return super.transfer( to, tokens );
  }

  function approve(address spender, uint tokens)
    public
    ifNotPaused
    returns (bool success) {
      return super.approve( spender, tokens );
  }

  function transferFrom(address from, address to, uint tokens)
    public
    ifNotPaused
    returns (bool success) {
      return super.transferFrom( from, to, tokens );
  }

  function mint( address _to, uint256 _amount )
    public
    ifNotPaused
    returns (bool) {
      return super.mint( _to, _amount );
  }

  function finishMinting()
    public
    ifNotPaused
    returns (bool) {
      return super.finishMinting();
  }

  function burn(uint256 _value)
    public
    ifNotPaused {
      super.burn( _value);
  }

  function burnFrom(address _from, uint256 _value)
    public
    ifNotPaused {
    super.burnFrom( _from, _value);
  }
}
