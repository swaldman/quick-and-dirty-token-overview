# Quick and Dirty ERC20 Token Overview

### Some notes from a workshop on the nuts and bolts of building tokens

## Table of Contents

* [Disclaimers](#disclaimers)
* [ERC20 Definition](#erc20-definition)
* [Code in this repository](#code-in-this-repository)
* [ERC20 (and Solidity smart contract) Security](#erc20-and-solidity-smart-contract-security)
* [Some other things to think about](#some-other-things-to-think-about)
* [How to play with the code](#how-to-play-with-the-code)
  - [Compile](#compile)
  - [Test (optional)](#test-optional)
  - [Deploy and interact on ropsten](#deploy-and-interact-on-ropsten)
  - [Appendix: Tabbability](#appendix-tabbability)

## Disclaimers

**&#X1F449; Quick and dirty means quick and dirty! Don't offer high-value solidity contracts to the world without being slow and audited!**

**&#X1F449; The code here is mostly just stolen (under MIT License) from the excellent work of the [Zeppelin](https://zeppelin.solutions) [team](https://zeppelinos.org),
            see [Open Zeppelin](https://github.com/OpenZeppelin/openzeppelin-solidity) and [Zeppelin OS](https://github.com/zeppelinos/zos).**

**&#X1F449; The Zeppelin OS Proxy library code is bleeding edge and not quite stable. Zeppelin is doing great work on safe-ish and resilient proxying, but
            it is work in progress.**

**&#X1F449; No effing warranties, of course, whether of my work or others'. Both the code herein and the exposition below are, um, quick and dirty, intended as a
            starting point, not an ending point.**

## ERC20 Definition

A good place to see the formal definition of the ERC20 standard, as well as an overview of how it works, is [here](https://theethereum.wiki/w/index.php/ERC20_Token_Standard).

## Code in this repository

1. [HelloWorld.sol](./src/main/solidity/HelloWorld.sol) &mdash; Just a quick Solidity Hello World
2. [UnsafeSimpleToken.sol](./src/main/solidity/UnsafeSimpleToken.sol) and [SafeSimpleToken.sol](./src/main/solidity/SafeSimpleToken.sol) &mdash; An implementation of the (very simple!) heart of a token contract, first with unsafe (but readable) native math operations, and then improved to use the [Open Zeppelin SafeMath Library](https://github.com/OpenZeppelin/openzeppelin-solidity/blob/master/contracts/math/SafeMath.sol)
3. [MintableBurnableERC20.sol](./src/main/solidity/MintableBurnableERC20.sol) &mdash; A fully functional implementation of an ERC 20 in almost no user code (relying instead upon the Open Zeppelin library)
4. [MintableBurnableERC20Spec.scala](./src/test/scala/quickanddirty/contract/MintableBurnableERC20Spec.scala) &mdash; A scala unit test of [MintableBurnableERC20.sol](./src/main/solidity/MintableBurnableERC20.sol). Relies upon predeployment of an instance of `MintableBurnableERC20.sol`, which this build defines via `Test / ethcfgAutoDeployContracts` in [build.sbt](./build.sbt#L23)
5. [ProxyableMintableBurnableERC20.sol](./src/main/solidity/ProxyableMintableBurnableERC20.sol), [PausableMintableBurnableERC20.sol](./src/main/solidity/PausableMintableBurnableERC20.sol), and [ZeppelinOsUpgradableProxyFactory.sol](./src/main/solidity/ZeppelinOsUpgradableProxyFactory.sol) &mdash; An exercise in desiging an ERC20 contract with an initializer externalized from the constructor (to support upgradability), and then an upgrading in place to a Pausable version of that contract using (work-in-progress) Zeppelin OS libraries.
6. [ProxiedMintableBurnableERC20Spec.scala](./src/test/scala/quickanddirty/contract/ProxiedMintableBurnableERC20Spec.scala) &mdash; A Scala unit test that deploys an upgradable proxy token without pausability, then upgrades it to a pausable version


## ERC20 (and Solidity smart contract) Security

For potentially high-value smart contracts, correctness and security should be prioritized over all other things. Getting Solidity smart-contacts "right"
has proven to be a challenging problem. One good thing is that everybody else's costly mistakes has formed a growing checklist of things you can try to guard against
when auditing your own code. Here are some items on that checklist, things you audit should be sure to examine.

### 1. Unsafe arithemtic

Solidity numerical data types are fixed length and can silently overflow or underflow. *Using built-in arithmetic operators is unsafe! You should almost never do it*
Instead use something like Open Zeppelin's [SafeMath](https://github.com/OpenZeppelin/openzeppelin-solidity/blob/master/contracts/math/SafeMath.sol).
Note that it is important to use safe operators even for things you consider relatively innocuous, like checks and tests, as well as obviously high-value
operations like mutations / transfers / etc.

Using safe arithmetic in the context of fixed-length type is the only way to be correct, but it itself (like most security techniques) can introduce its own
hazards. In particular, "liveness" can become an issue if users can create a condition where otherwise desirable operations must trigger an overflow. For
example, imagine a contract that allows users to supply a sequence number with some operation, with the restriction that a new sequence number must be higher
than any that the contract has aleady seen, or that autogenerates a sequence number if none is provided by incrementing the last. A user can then kill the
contract simply by conducting a tranaction and providing the maximum value of the sequence-number's type as its sequece number. Users will not be able to provide
their own new sequence number, and the contract will trigger overflows and revert state if it tries internaly to increment the sequence number.

### 2. **Computatation exceeding block gas limit**

An Ethereum transactions must be incorporated into a block, but a block is permitted to represent only a limited amount of computation. The block "gas limit" [varies
over time](https://etherscan.io/chart/gaslimit), but any proposed tranaction that exceeds it will not be incorporated (until some perhaps hypothetical future when
the block limit becomes long enough, in the unlikely event the transaction is still valid).

**You must take care to avoid code that may result in unbounded or unlimited computation.**

Most commonly, this means you should avoid unlimited iteration. If you have
a contract, for example, that at some "maturity" pays off a variable-length list of stakeholders based on interactions with the contract prior to maturity, you may
find that no one can be paid if the list has grown too large for the interation to fit within the block gas limit. The contract's funds may be permanently tracked.

(In general in such situations it's best to adopt a right-to-withdraw rather than payout approach. When the contract is mature, each stakeholder obtains a right to
individually withdraw their balance in their own, separate tranaction. Thus no iteration is required within the contact to disburse funds.]

### 3. Overly public or insufficiently guarded functions

Functions are public by default in current versions of Solidity. (This was not a great design choice, I think.) Often contracts will include functions
intended to be called only within the contract itself, or when the contract is in a certain state (such as "uninitialized"), or by certain callers.

Functions that are not intended for public use should be marked `private` (or `internal` if it is intended that inheriting implementations might call them).

Functions that must only be called when a function is in a certain state or by certain callers should be guarded using Solidity's `require` statement, or
if the same requirement obtains for multiple functions, via a custom Solidity modifier that guards function bodies behind a `require` statement. (Custom
modifiers are a really nice Solidity design choice, IMHO.)

### 4. Incautious delegation

The EVM permits contracts to delegate their computation to some other contract, rather to perform the computation themselves, if their storage is compatible. This functionality is known
as "delegatecall". Delegation can be desirable for a couple of reasons:

   * It is expensive to deploy code. A full implementation can be expensively deployed just once. Later instances can be cheaply deployed, with just a tiny bit of code to delegate implementations.
   * Delegation is one technique (the main one in use) to design "immutable" smart contracts to be "upgradable". While the contract's code _is_ immutable and never changes,
     the address to which the implementation of the contract is delegated can be upgraded
     
However, delegation is dangerous!

   * Universal delegation, whereby all function calls are forwarded to the delegate via its fallback function, exposes all functions of the delegate contract to
all users, which may include functionaliy users of proxy contracts should not be able to access. **Access to delegate functions should be carefully guarded.**
   * Delegation will yield disruptive, almost certainly undesirable behavior, if the storage of the stub and the delegate are not compatible. "Upgrading" a contract
     to a delegate with incompatible storage will yield caastrophic results.
   * Solidity's built-in `delegatecall` and `call` functions return 0, but do not revert, if there is no code at the address of the delegate or callee or if the call fails.
     This is bad -- you have to check for failures, and you have lost the return value of the function if the function succeeds. Delegation is now nearly always implemented
     via in-line Solidity assemply using the DELEGATECALL EVM opcode directly, so that failures `revert` and return values are preserved. This is the right thing to do, but
     inline assembly code is dangerous and unchecked by the Solidity compiler. When working with sharp power tools, work with care.
   * Contracts often offer both directly implmemented functions as well as universal "fallback" delegation -- functions not directly implemented fallback to
     a separate implementation. However, functions are identified by only four bytes, and it is possible that there might be a collision between the identifier of a
     directly implemented function and one in the fallback contract, even though they appear distinct. In that case, the fallback contract implementation will be unexpectedly
     "shadowed" by an unrelated main contract function. If such mixed delegation contracts are upgradable, malicious actors can try intentionally to cause upgrades that include
     collisions, breaking the delegate contract in difficult to predict ways and potentially allowing all kinds of mischief. See
     [Nomic Labs writup](https://medium.com/nomic-labs-blog/malicious-backdoors-in-ethereum-proxies-62629adf3357).
   * If the contact you delegate to self-destructs (and if the address to whichyou delegate is not upgradable), your contract is broken.

If you use a delegation pattern, to reduce deployment expense, to enable upgradeability, or both. Be very, very careful about all of these issues (and others we may not yet have found)!

(Note that upgradability techniques often require initialization functions distinct from contract constructors. Be very careful to restrict access to these initializers,
so that only the upgrading party can call them and they cannot be called more than once!)

### 5. Sending ETH can be dangerous

_Note: An ERC-20 token usually does not need to send or receive ETH._

When you try to send ETH from a smart contract, your attempt to send may not work, or else it may work but much more than you bargained for may occur.

Any time a contract sends to a smart contract, it is in fact calling a function on the recipient, and that function can do anything at all. Sending represents
a loss of control to the caller. Alernatively, the send may fail, which may break assumptions of the calling contract to bad effect.

There are three common ways to send:
   1. `<address>.send(uint256 amount) returns (bool)`
   2. `<address>.transfer(uint256 amount)`
   3. `<address>.call.value(uint256 amount)() returns (bool)`

`<address>.send(uint256 amount) returns (bool)` and `<address>.transfer(uint256 amount) returns (bool)` both limit the amount of computation a recipient might do
in response to a payment, because they forward precisely 2300 gas to receipients. Using `call()` by default forwards all available gas (or you can set a custom
gas limit to forward). `transfer` reverts the transaction if the attempt to send fails. `send` and `call` do not revert, be careful to check the return value to know
whether the attempt succeeded.

In general, contracts should keep track of ether owed and let users withdraw via simple functions that do little more than send eth and debit the balance owed.
It's dangerous to attempt to send in richer codepaths that do essential work for the contract. Since a send can always fail, if you intermingle calls to `transfer` with contract
language, a bad payee can prevent the logic of your contract from executing. Usually it's best to use `send`, and to be vigilant about checking the return value to
react properly if the attempt to send fails.

Especially if you use the gas-rich call function, remember that you have handed over control to the payee when you make a payment, and the payee might do
anything -- including calling functions on your contract that change its state -- between your call to send and when it returns. Ideally, you should avoid doing
anything that might rely upon contract state after send. It is recommended to use the "checks, effects, interactions" pattern. Check contract state, perform any
modification of contract state that must be performed, and only afterwards "interact" -- make payments or call functions on other contracts. As long as interactions
are the last thing you do, and you don't need to update state in response to those interactions, then any callbacks
back into the contract from interactions can be considered to have occurred logically after your own function has completed.

### 6. Reentrancy

It is not only sending ether that surrenders control to another caller who might unexpectedly "re-enter", or call functions on the contract while another function
remains in process. Any time a contract calls calls a function on another contract, such a callback could occur. Just as with payments, contract authors should treat
calls to contracts outside of their own authorship and control as potentially malicious re-enterers, an should strive to adhere to the "checks, effects, interactions"
pattern.

### 7. Unexpected excess ETH or token balances

Contracts can refuse to accept payments in ETH by failing to mark any function (including the default, fallback function) as `payable`. However, their are two ways to
circumvent this restriction. Payments will occur regardless of the presence of any `payable` function if

a. A miner has specified the contract's address as the address to which block rewards should be paid
b. A contract calls `selfdestruct` (or `suicide`), specifying the address of the otherwise nonpayable contract as refund address

Since there is no way to prevent these unexpected ETH deposits to a contract, contracts should not rely on their ETH balance as being only
the payments they have accepted as ordinary payments or within functions, less any payouts. The ETH balance will be no less than this amount,
but it may be greater.

Similarly, a contract has no control over how many ERC20 tokens its address receives. While the contract may (using the `approve` / `transferFrom` mechanism)
control some inflows of tokens, other parties may add to a contract's token balance without any notification of or capacity to refuse by the recipient contract.

### 8. Frozen token balances

Unless a smart contract expects to receive and is coded to interact with an ERC20 token, tokens sent to that contract's address will usually be frozen forever.
ERC 20 token contracts have no way of vetoing transfers to them. (Successors to the ERC 20 standard hope to add such vetos.)

It is possible to code contracts such that some "owner" can withdraw unexpected token balances sent to the contract.

### 9. Inheritance linearization confusions

Solidity supports multiple inheritance, and customization via inheritance and mixins is quite common. Under the covers, Solidity uses [C3 Linearization](https://en.wikipedia.org/wiki/C3_linearization)
to order the inheritance relationship. However, while the linearization algorithm does its job of imposing a unique ordering of contracts (from base to derived), as hierarchies
grow complicated, the results come to [defy intuition](https://ethereum.stackexchange.com/questions/56802/a-solidity-linearization-puzzle).

Misunderstanding the ordering of contract inheritance can [lead to subtle bugs](https://pdaian.com/blog/solidity-anti-patterns-fun-with-inheritance-dag-abuse/).
The storage layout of contracts is also a function of linearization, and delegation patterns require consistent storage layouts. When using delegation to
support upgradability, one must be sure that the storage layout is preserved (although perhaps extended) by the new contract. Usually this means the ordering of storage-affecting
compilation units must be preserved.
 
### 10. Be creatively paranoid

Everything that can be exploited will be exploited. No checklist can capture it all.

## Some other things to think about

### Pausability or freezability

It may be useful to have an address (which may represent an authentication policy, rather than a single private key, as in a multisig wallet)
that can pause or freeze the mutators (non-read-only) functions of a token in case the contract needs to be supeceded or something goes badly
wrong. It may be useful for the "pauser" of the contract to be distinct from the "owner" who can mint and redeem, as a backstop in case the
keys of the minter/redeemer are compromised.

### Whitelists and blacklists

Some tokens protect some functions (especially minting) with a whitelist, and allow for a blacklist presence on which effectively
freezes a token account.

### Contract-owner initiated transfers

When a token is not intended to represent a "trustless" app, but is a liability of the centralized organization, it may be useful for the
centralized organization to be able to authenticate transfers of the tokens between arbitrary accounts. That way, a class of users of the
token who interact only with the sponsoring organization, but not with the Ethereum network or even Ethereum primitives like signatures,
may be supported.

Users of a token that supports this "feature" obviously put a great deal of trust in the owner or sponsor of the contract. However, all
transfers made in this way would be public and immutably auditable on the blockchain, so if the sponsor is accountable via legal and
regulatory channels, users of the token may consider the convenience worth the risk.

This is entirely inappropriate, however, for tokens intended to represent claims on "trustless" or decentralized applications.

### Batching

There may be some savings, in cost and network overhead, to batching multiple transactions &mdash; transfers, mints, and burns &mdash;
into a single transaction. One way to do this is to add batch methods to the token contract itself. Infamously, however, this was the path
by which [a catastrophic overflow bug was introduced into a bunch of well-known tokens](https://medium.com/@peckshield/alert-new-batchoverflow-bug-in-multiple-erc20-smart-contracts-cve-2018-10299-511067db6536),
so it has a bit of a bad reputation. However, a batch function can be properly rather than improperly coded.

It's worth noting, however, that one can avoid the extra complexity and high-stakes auditing that goes along with adding new functions to the token
contract and still get the benefits of batching. Just leave the batch functionality out of the token contract itself, but deploy a separate "Batcher" contract,
and let that contract make calls on the token itself. Something like the following (quick and dirty and never compiled!) pseudocode...
```
contract MintableERC20 {
   function mint( address receiver, uint amount ) returns (bool);
}
contract Batcher {
  function batchMint( address mintableToken, address[] receivers, address[] amounts ) {
    uint len = receivers,length;
    require( amounts.length == len );
    for (uint i = 0; i < len; ++i ) {
       bool check = MintableERC20(mintableToken).mint( receivers[i], amounts[i] );
       if (!check) revert();
    }
  }
}
```

### "Advanced" token standards

At least two prominent proposals have been made to replace the ERC 20 standard, [ERC-223](https://github.com/ethereum/EIPs/issues/223) and [EIP-777](https://github.com/ethereum/EIPs/blob/master/EIPS/eip-777.md).
Neither has yet "caught on", but one may. (My suspicion is that 777 is more likely, but things are moving slowly.) It's worth thinking about how one would support these evolving standards.

## How to play with the code

This is an `sbt-ethereum` project. It tries to be as portable as possible. You should be able to clone this repository from github and immediately follow the
instructions below. The only prerequisite is an installed Java 8 virtual machine in the path.

When you get annoyed by the ridiculously long command names (you will!), please read the appendix on [tabbability](#appendix-tabbability).

#### Compile

To compile the code, make sure a Java 8 virtual machine is installed on your computer, then run `./sbtw` from the base directory of this repository. The first time you run this, lots of suff will get
downloaded and it will be slow. You will be prompted to create a wallet, to let it be the default sender for the blockchain 'ropsten', and to install a solidity compiler. Do all of those things. Then...

```
sbt:quick-and-dirty-token-overview> compile

```
#### Test (optional)

To run the unit tests, you will need `ganache-cli` installed on your machine, which is a node.js application. If you are set up for that kind of thing...

```
npm install -g ganache-cli
```

For more information on ganache, look [here](https://truffleframework.com/docs/ganache/quickstart).

Once `ganache-cli` is installed and in your path, you should be able to...

```
sbt:quick-and-dirty-token-overview> ethDebugGanacheTest
```

which will deploy the solidity smart-contracts in a local, temporary environment and run the unit tests.

#### Deploy and interact on ropsten

Before you can deploy contracts defined in this package, you will need to get some Ropsten test ether from a faucet. Try or https://blog.b9lab.com/when-we-first-built-our-faucet-we-deployed-it-on-the-morden-testnet-70bfbf4e317e (I recommend "script ninja mode") or https://faucet.ropsten.be. Use the account you created on startup.
If you have forgotten it, try...
```
sbt:quick-and-dirty-token-overview> ethKeystoreList
```

Once you have generated a transaction from the faucet, follow it (via the transaction hash that is generated) at http://ropsten.etherscan.io/

When the transaction is mined (no longer pending), you should be able to see a balance:
```
sbt:quick-and-dirty-token-overview> ethAddressBalance
```

If you have not alreay compiled the contacts in the project, do so:

```
sbt:quick-and-dirty-token-overview> compile
```

Now you can try to deploy.
```
sbt:quick-and-dirty-token-overview> ethTransactionDeploy MintableBurnableERC20
```

If deployment hangs for a very long time, it probably means the gas price estimated by the node is too low. You can <ctrl-c> to interrupt, rerun `./sbtw`, and then try...
```
sbt:quick-and-dirty-token-overview> ethGasPriceOverrideSet 5 gwei
```
and then

```
sbt:quick-and-dirty-token-overview> ethTransactionDeploy MintableBurnableERC20
```

Once deployment completes, sbt-ethereum will prompt you to give the new contract address an alias, if you wish. You can verify that the contract is deployed
using

```
sbt:quick-and-dirty-token-overview> ethContractCompilationList
```

It's the one with a deployment address. If the list is long and ugly, clean up undeployed compilations with
```
sbt:quick-and-dirty-token-overview> ethContractCompilationCull
```

and then list again. If you set an alias for the contact, check that with

```
sbt:quick-and-dirty-token-overview> ethAddressAliasList
```

Once you have a deployed contact, try

```
sbt:quick-and-dirty-token-overview> ethTransactionView 0x906245d8ceae99c83947eaf81cdf6716a80950c7 <tab>
```

but with the address **or the alias** of your newly deployed contract. You should see the read-only methods of the contract. Try some. Once you have entered a method name,
hit tab to be prompted for arguments, for functions that require arguments. Try some.

The results of all these functions will basically be zero on this newly deployed token contract. To change that, you'll have to call transactions that
modify the blockchain, functions like `mint` and `transfer`. If you needed to set a gas override to get your deployment transaction to mine, make sure that is still
in effect (it should be, if you haven't quit the session) with
```
sbt:quick-and-dirty-token-overview> ethGasPriceOverridePrint
```

To reset it, use again
```
sbt:quick-and-dirty-token-overview> ethGasPriceOverrideSet 5 gwei
```

(Try `ethGasPriceOverride`<tab> to see all the `ethGasPriceOverride`-related commands.)

Now try something like

```
ethTransactionInvoke 0x906245d8ceae99c83947eaf81cdf6716a80950c7 <tab>
```

replacing `0x906245d8ceae99c83947eaf81cdf6716a80950c7` with your newly deployed contract's address **or alias**.

You should see more functions available, including `mint`.

Let's mint some tokens to the `defaultSender` (from which we've been sending our transactions):
```
sbt:quick-and-dirty-token-overview> ethTransactionInvoke 0x906245d8ceae99c83947eaf81cdf6716a80950c7 mint defaultSender 10000000
```

Again, replace `0x906245d8ceae99c83947eaf81cdf6716a80950c7` with your newly deployed contract's address **or alias**.

Now see if you can `ethTransactionView` to see the new `totalSupply`, as well as the `balanceOf` `defaultSender`.

If you want to start transferring tokens, you can just transfer to random addresses, but then your tokens will be lost for good. If you want to be able to
transfer them back, you can create more addresses and wallets. Try

```
sbt:quick-and-dirty-token-overview> ethKeystoreWalletV3Create
```

This command does noy (yet) automatically prompt for an alias. If you want to give it one, use

```
sbt:quick-and-dirty-token-overview> ethAddressAliasSet <alias> <address-hex>
```

replacing `<alias>` and `<address-hex>` with appropriate values.

To send transactions from addresses other than the `defaultSender` you defined initially, use
```
sbt:quick-and-dirty-token-overview> ethAddressSenderOverrideSet <address-hex-or-alias>
```

Have fun!

#### Appendix: Tabbability

**_sbt-ethereum is designed around tabbability._ The command names are ridiculously verbose and annoying long, but there is method to the madness. They are
designed so that at each "level" you have to type only a single character to tab to the next level.**

For example, suppose you want to type the ridiculously long command `ethAddressSenderOverrideSet`. You don't actually type that. Your fingers would fall off.
You end up typing just `ethA<tab>S<tab>O<tab>S`. But you don't have to remember that! Type `eth<tab>` and you'll see a lot (too many) commands. Choose the subcommand
that seems relevant (`Address`), it starts with an `A`, so type `A<tab>`. Then type `<tab>` again and you'll see something like
```
ethAddressAliasDrop             ethAddressAliasList             ethAddressAliasSet              ethAddressBalance
ethAddressPing                  ethAddressSenderEffective       ethAddressSenderOverrideDrop    ethAddressSenderOverridePrint
ethAddressSenderOverrideSet     
```

You are trying to change the sending address, so you will continue with `S<tab>`. Press `<tab> again.
```
ethAddressSenderEffective       ethAddressSenderOverrideDrop    ethAddressSenderOverridePrint   ethAddressSenderOverrideSet     
```

You want to set an override, so you will type `O<tab>` and then `S<tab>`, and finally you will have completed the command.

When the command is complete, press `<space>` and then `<tab>` again:
```
<address-hex>       <ens-name>.eth      defaultSender       mintable-burnable   
```

You will see what the command expects as its next argument, if any. If no more arguments are expected, you will see the string `{invalid input}`, and you
should just hit return to execute the command, without typing anything more.
