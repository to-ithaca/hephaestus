# hephaestus
## How To Build
 1. Download the Vulkan SDK from [LunarXchange](https://vulkan.lunarg.com/)
 2. Build the Vulkan SDK by running `./setup_env.sh`, `./build_samples.sh` and `./build_examples.sh`
 3. Check that your graphics card supports vulkan by running `vulkaninfo`
 4. Set the `VULKAN_SDK` environment variable to the `VulkanSDK/version/arch` directory
 5. Clone [GLFW](https://github.com/glfw/glfw.git)
 6. In glfw execute:

 ```bash
 cmake .
 make
 ```
 
 You may see warnings such as `cannot find Vulkan`.  Ignore these.
 7. Build this project with `sbt clean compile`.  There should be no cmake errors.
