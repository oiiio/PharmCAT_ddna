"""

Use chatGPT to take a recommendation descriptions and return only one of three guidance levels (1,2,3) and string

"""

import openai
import os
import json
import sys

# Load the OpenAI API key from an environment variable or a file
