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


package quickanddirty.contract

import org.specs2._
import Testing._

import com.mchange.sc.v1.consuela._
import com.mchange.sc.v1.consuela.ethereum.stub
import com.mchange.sc.v1.consuela.ethereum.stub.sol

import com.mchange.sc.v1.consuela.ethereum.jsonrpc.Client.BlockNumber

import scala.concurrent.Await
import scala.concurrent.duration._

class ProxiedMintableBurnableERC20Spec extends Specification with AutoSender { def is = sequential ^ s2"""
  Our upgradable proxy of a MintableBurnableERC20...
     the contract we use as original implementation should have default sender as owner     ${e0}
     should start with a zero balance (regardless of its implementation's balance)          ${e1}
     minting 1000000 units yields total supply of 1000000                                   ${e2}
     minting recipient's balance should be 1000000                                          ${e3}
     recipient burning 500000 units leaves balance of 500000                                ${e4}
     should have as its owner the random owner we have selected                             ${e5}
     should have as its admin the random admin we have selected                             ${e6}
     should have as its implementation the original implementation we have chosen           ${e7}
     should not be pausable until we upgrade                                                ${e8}
     should have new, pausable implementation after we upgrade                              ${e9}
     should be pausable after we upgrade                                                    ${e10}
     should not be modifiable when paused                                                   ${e11}
     should have retained the minting recipient's 500000 balance from prior to the upgrade  ${e12}
     should be unpausable                                                                   ${e13}  
     token recipient burning 100000 after unpausing leaves a balance of 400000              ${e14}
     token recipient transferring 200000 to owners leaves each with a balance of 200000     ${e15}  
  """

  /*
   * 
   * These are all set-up via `Test / ethcfgAutoDeployContracts` in build.sbt
   * 
   */ 

  val originalImplementation = MintableBurnableERC20( TestSender(0).contractAddress(1) ) // autodeployed ProxyableMintableBurnableERC20Spec

  val upgradeabilityProxyFactory = UpgradeabilityProxyFactory( TestSender(0).contractAddress(2) )

  val pausableImplementation = PausableMintableBurnableERC20( TestSender(0).contractAddress(3) )

  def newFundedRandomSender() = {
    val out = createRandomSender()
    Await.ready( Faucet.sendWei( out.address, sol.UInt256(1.ether) ), Duration.Inf )
    out
  }

  val randomAdmin  = newFundedRandomSender()
  val randomOwner  = newFundedRandomSender()
  val randomUser   = newFundedRandomSender()

  // now where did i get this from???
  val initializationData = s"0xc4d66de8000000000000000000000000${randomOwner.address.hex}".decodeHexAsSeq

  // this is lots more of a pain than I'd expected
  lazy val adminProxy = {
    val proxyInfo : stub.TransactionInfo = upgradeabilityProxyFactory.transaction.createProxyAndCall( randomAdmin.address, originalImplementation.contractAddress, initializationData )
    val proxyCreatedEvent = UpgradeabilityProxyFactory.Event.ProxyCreated.collect( proxyInfo ).ensuring( _.size == 1 ).head // check that we have just one event, as expected, then get it
    AdminUpgradeabilityProxy( proxyCreatedEvent.proxy )
  }
  lazy val tokenProxy = ProxyableMintableBurnableERC20( adminProxy.contractAddress )

  lazy val pausableTokenProxy = PausableMintableBurnableERC20( adminProxy.contractAddress ) // this shouldn't actually be pausable until we upgrade!

  def e0 : Boolean = originalImplementation.constant.owner() == DefaultSender.address

  def e1 : Boolean = tokenProxy.constant.totalSupply().widen == 0

  def e2 : Boolean = {
    tokenProxy.transaction.mint( randomUser.address, sol.UInt256(1000000) )( sender = randomOwner )
    tokenProxy.constant.totalSupply().widen == 1000000
  }

  def e3 : Boolean = tokenProxy.constant.balanceOf( randomUser.address ).widen == 1000000

  def e4 : Boolean = {
    tokenProxy.transaction.burn( sol.UInt256(500000) )( sender = randomUser )
    tokenProxy.constant.balanceOf( randomUser.address ).widen == 500000
  }

  def e5 : Boolean = tokenProxy.constant.owner() == randomOwner.address
  
  def e6 : Boolean = adminProxy.constant.admin()( sender = randomAdmin ) == randomAdmin.address
  
  def e7 : Boolean = adminProxy.constant.implementation()( sender = randomAdmin ) == originalImplementation.contractAddress

  def e8 : Boolean = {
    try {
      pausableTokenProxy.transaction.pause()( sender = randomOwner )
      false
    }
    catch {
      case e : Exception => true
    }
  }

  def e9 : Boolean = {
    adminProxy.transaction.upgradeTo( pausableImplementation.contractAddress )( sender = randomAdmin )
    adminProxy.constant.implementation()( sender = randomAdmin ) == pausableImplementation.contractAddress
  }

  def e10 : Boolean = {
    pausableTokenProxy.transaction.pause()( sender = randomOwner )
    pausableTokenProxy.constant.paused()( sender = randomOwner ) == true
  }

  def e11 : Boolean = {
    try {
      pausableTokenProxy.transaction.burn( sol.UInt256(100000) )( sender = randomUser )
      false
    }
    catch {
      case e : Exception => true
    }
  }

  def e12 : Boolean = tokenProxy.constant.balanceOf( randomUser.address ).widen == 500000

  def e13 : Boolean = {
    pausableTokenProxy.transaction.unpause()( sender = randomOwner )
    pausableTokenProxy.constant.paused()( sender = randomOwner ) == false
  }

  def e14 : Boolean = {
    pausableTokenProxy.transaction.burn( sol.UInt256(100000) )( sender = randomUser )
    pausableTokenProxy.constant.balanceOf( randomUser.address ).widen == 400000
  }

  def e15 : Boolean = {
    pausableTokenProxy.transaction.transfer( randomOwner.address, sol.UInt256(200000) )( sender = randomUser )
    pausableTokenProxy.constant.balanceOf( randomUser.address ).widen == 200000 && pausableTokenProxy.constant.balanceOf( randomOwner.address ).widen == 200000
  }
}
