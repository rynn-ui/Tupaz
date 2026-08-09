package com.tupaz.data.manifest

import com.tupaz.domain.pipeline.PipelineStageType
import com.tupaz.domain.pipeline.ProcessingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class PipelineManifestParserTest {

    private lateinit var parser: PipelineManifestParser

    @Before
    fun setUp() {
        parser = PipelineManifestParser()
    }

    @Test
    fun `parse valid json correctly constructs PipelineManifest`() {
        val json = """
            {
              "version": 1,
              "modes": [
                {
                  "mode_id": "balanced",
                  "scale_factor": 2,
                  "stages": [
                    {
                      "stage_id": "esrgan_2x",
                      "type": "upscale",
                      "model_name": "realesr-animevideov3-x2.bin",
                      "enabled": true,
                      "parameters": {
                        "tile_size": "384"
                      }
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        val manifest = parser.parse(json)
        assertEquals(1, manifest.version)
        assertEquals(1, manifest.modes.size)

        val modeConfig = manifest.findModeConfig(ProcessingMode.BALANCED)
        assertNotNull(modeConfig)
        assertEquals("balanced", modeConfig!!.modeId)
        assertEquals(2, modeConfig.scaleFactor)
        assertEquals(1, modeConfig.stages.size)

        val stage = modeConfig.stages[0]
        assertEquals("esrgan_2x", stage.stageId)
        assertEquals(PipelineStageType.UPSCALE, stage.type)
        assertEquals("realesr-animevideov3-x2.bin", stage.modelName)
        assertTrue(stage.enabled)
        assertEquals("384", stage.parameters["tile_size"])
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse blank json throws IllegalArgumentException`() {
        parser.parse("  ")
    }

    @Test(expected = IllegalArgumentException::class)
    fun `parse malformed json throws IllegalArgumentException`() {
        parser.parse("{ invalid json }")
    }
}
