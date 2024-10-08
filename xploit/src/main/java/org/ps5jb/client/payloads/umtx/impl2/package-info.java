/**
 * UMTX bug triggering implemented by
 * <a href="https://github.com/cheburek3000/bdj-sdk/blob/umtx/samples/ps5-payload-loader/src/org/homebrew/umtx/Exploit.java">cheburek3000</a>
 * and adapted to run on this SDK.
 *
 * To create a JAR with this payload, add
 * <pre>-Dxploit.payload=org.ps5jb.client.payloads.umtx.impl2.UmtxExploit</pre>
 * when building the remote JAR.
 */
package org.ps5jb.client.payloads.umtx.impl2;