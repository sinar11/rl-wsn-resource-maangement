// @(#)Extension.java   9/2002
// Copyright (c) 1998-2002, Distributed Real-time Computing Lab (DRCL) 
// All rights reserved.
// 
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions are met:
// 
// 1. Redistributions of source code must retain the above copyright notice,
//    this list of conditions and the following disclaimer. 
// 2. Redistributions in binary form must reproduce the above copyright notice,
//    this list of conditions and the following disclaimer in the documentation
//    and/or other materials provided with the distribution. 
// 3. Neither the name of "DRCL" nor the names of its contributors may be used
//    to endorse or promote products derived from this software without specific
//    prior written permission. 
// 
// THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
// AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR
// ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
// SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
// CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
// OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
// OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
// 

package drcl.comp;

/**
 * An extension is a component that behaves as part of other components
 * in the sense that when data arrives at an extension, instead of
 * a new thread is created to handle the data, the thread at the sending
 * component carries over the component boundary.
 *
 * This is achieved by disabling the execution boundary flag of
 * all the ports of an extension component.
 * Note that subclasses must not turn off the "PortNotification" flag
 * and must call <code>super.portAdded(Port)</code> when overriding
 * the method, as this component makes use of them to disable the
 * execution boundary flag of all ports added.
 *
 * @author Hung-ying Tyan
 */
public class Extension extends Component
{
	{ setPortNotificationEnabled(true); }
	
	public Extension() 
	{ super(); 	}
	
	public Extension(String id_)
	{ super(id_); }
	
	protected void portAdded(Port p_)
	{ p_.setExecutionBoundary(false); }

	public void setExtensionEnabled(boolean enabled_)
	{ setPortNotificationEnabled(enabled_); }

	public boolean isExtensionEnabled()
	{ return isPortNotificationEnabled(); }
}
