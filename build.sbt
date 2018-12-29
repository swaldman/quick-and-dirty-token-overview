name := "quick-and-dirty-token-overview"

version := "0.0.1-SNAPSHOT"

ethcfgScalaStubsPackage := "quickanddirty.contract"

ethcfgNodeChainId := 3 // ropsten

ethcfgNodeUrl := {
  try {
    val infuraToken = sys.env("ETH_INFURA_TOKEN")
    s"""https://ropsten.infura.io/${infuraToken}"""
  }
  catch {
    case t : Throwable => println( "WARNING: No Infura API Token found, Ethereum JSON-RPC calls may fail. If they do, set environment variable ETH_INFURA_TOKEN and rerun." )
    "https://ropsten.infura.io/"
  }
}

// only necessary while using a SNAPSHOT version of sbt-ethereum
resolvers += ("snapshots" at "https://oss.sonatype.org/content/repositories/snapshots")

Test / ethcfgAutoDeployContracts := Seq( "MintableBurnableERC20", "ProxyableMintableBurnableERC20", "UpgradeabilityProxyFactory", "PausableMintableBurnableERC20" )

Test / parallelExecution := false

libraryDependencies += "org.specs2" %% "specs2-core" % "4.0.2" % "test"





