/**
 * This package contains classes which offer a Java native array
 * view over native memory. This technique seems to work on PS5
 * but is rather unstable in other environments due to the fact
 * that JVM garbage collector attempts to operate on these
 * simulated native types and predictably crashes.
 */
package org.ps5jb.sdk.core.dataview;
