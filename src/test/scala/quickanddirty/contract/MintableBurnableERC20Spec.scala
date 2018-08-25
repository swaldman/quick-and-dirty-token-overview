package quickanddirty.contract

import org.specs2._
import Testing._

import com.mchange.sc.v1.consuela.ethereum.stub
import com.mchange.sc.v1.consuela.ethereum.stub.sol

class MintableBurnableERC20Spec extends Specification with AutoSender { def is = sequential ^ s2"""
  A MintableBurnableERC20...
     should start with a zero balance                           ${e1}
     minting 1000000 units yields total supply of 1000000       ${e2}
     minting recipient's balance should be 1000000              ${e3}
     recipient burning 500000 units leaves balance of 500000    ${e4}
  """

  /*
   * 
   * This is set-up via `Test / ethcfgAutoDeployContracts` in build.sbt
   * 
   */ 
  val tokenContract = MintableBurnableERC20( TestSender(0).contractAddress(0) )

  val randomSender = { // yuk
    import scala.concurrent.Await
    import scala.concurrent.duration._

    val out = createRandomSender()
    Await.ready( Faucet.sendWei( out.address, sol.UInt256(1.ether) ), Duration.Inf )
    out
  }

  def e1 : Boolean = tokenContract.constant.totalSupply().widen == 0

  def e2 : Boolean = {
    tokenContract.transaction.mint( randomSender.address, sol.UInt256(1000000) )
    tokenContract.constant.totalSupply().widen == 1000000
  }

  def e3 : Boolean = tokenContract.constant.balanceOf( randomSender.address ).widen == 1000000

  def e4 : Boolean = {
    tokenContract.transaction.burn( sol.UInt256(500000) )( sender = randomSender )
    tokenContract.constant.balanceOf( randomSender.address ).widen == 500000
  }
}
