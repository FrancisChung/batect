/*
   Copyright 2017-2018 Charles Korn.

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

package batect.execution.model.events

import batect.docker.DockerImage
import com.natpryce.hamkrest.equalTo
import org.jetbrains.spek.api.Spek
import org.jetbrains.spek.api.dsl.describe
import org.jetbrains.spek.api.dsl.it
import org.jetbrains.spek.api.dsl.on

object ImageBuiltEventSpec : Spek({
    describe("an 'image built' event") {
        val image = DockerImage("image-1")
        val event = ImageBuiltEvent("/some-build-dir", image)

        on("toString()") {
            it("returns a human-readable representation of itself") {
                com.natpryce.hamkrest.assertion.assertThat(event.toString(), equalTo("ImageBuiltEvent(build directory: '/some-build-dir', image: 'image-1')"))
            }
        }
    }
})