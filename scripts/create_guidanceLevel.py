"""

Use chatGPT to take a recommendation descriptions and return only one of three guidance levels (1,2,3) and string

"""

import openai
import os
import json
import time

openai.api_key = os.getenv("OPENAI_API_KEY")

from openai import OpenAI
client = OpenAI()
client.api_key = os.getenv("OPENAI_API_KEY")

files_completed = []
#read file names to see which are done already
with open("files_completed.txt", "r") as x:
    for line in x:
        files_completed.append(line.strip())



# Open and loop through every JSON file in the /Users/gareth/PharmCAT_ddna/src/main/resources/org/pharmgkb/pharmcat/reporter/guidelines directory
# For each file, loop through each object and for each recommendations element, send the element's ["text"]["html"] to the OpenAI API as the user message
for filename in os.listdir("/Users/gareth/PharmCAT_ddna/src/main/resources/org/pharmgkb/pharmcat/reporter/guidelines"):
    if filename.endswith(".json") and filename not in files_completed:
        with open(f"/Users/gareth/PharmCAT_ddna/src/main/resources/org/pharmgkb/pharmcat/reporter/guidelines/{filename}") as f:
            data = json.load(f)
            print(f)
            i = 0
            for recommendation in data["recommendations"]:
                completion = client.chat.completions.create(
                    model="gpt-3.5-turbo",
                    messages=[
                    {
                        "role": "system", 
                        "content": "You are a pharmacogenomic expert assigning one of three guidance levels to a recommendation description that will guide treatment. You will see a description of a pharmacogenomic guideline and need to assign a level to it. Your only output will be the level number (1, 2, or 3).\n\n\
                            Level meanings:\n\n\
                                1 - Use standard precautions. Either there is no recommendation given by the description or genotype shows no elevated risks or treatment-altering indicators for prescribing the medication according to standard practice.\n\
                                2 - Should be used with monitoring. There are some optional suggestions provided to mitigate risk because the risk factor is moderate.\n\
                                3 - Significant changes or risks. The recommendation indicates important changes in efficacy, toxicity, or other properties for the specified medication. The recommendation strongly indicates to avoid and use an alternate medication.\
                                "     
                        },
                    {"role": "user", "content": recommendation["text"]["html"]}
                    ]
                )
                print(recommendation["text"]["html"])
                print(completion.choices[0].message.content)
                data["recommendations"][i]["guidanceLevel"] = str(completion.choices[0].message.content)
                i += 1

                #sleep to allow for API rate limiting
                time.sleep(1)
        with open(f"/Users/gareth/PharmCAT_ddna/src/main/resources/org/pharmgkb/pharmcat/reporter/guidelines/{filename}", "w") as o:
            json.dump(data, o, indent=4)
        files_completed.append(filename)

#write to file to keep track of which files have been completed
with open("files_completed.txt", "w") as y:
    for file in files_completed:
        y.write(file + "\n")


"""
completion = client.chat.completions.create(
  model="gpt-3.5-turbo",
  messages=[
    {
        "role": "system", 
        "content": "You are a pharmacogenomic expert assigning one of three guidance levels to a recommendation description that will guide treatment. You will see a description of a pharmacogenomic guideline and need to assign a level to it. Your only output will be the level number (1, 2, or 3).\n\n\
            Level meanings:\n\n\
                1 - Use standard precautions. Either there is no recommendation given by the description or genotype shows no elevated risks or treatment-altering indicators for prescribing the medication according to standard practice.\n\
                2 - Should be used with monitoring. There are some optional suggestions provided to mitigate risk because the risk factor is moderate.\n\
                3 - Significant changes or risks. The recommendation indicates important changes in efficacy, toxicity, or other properties for the specified medication. The recommendation strongly indicates to avoid and use an alternate medication.\
                "     
     },
    {"role": "user", "content": "<p>Choose an alternative or start with 10% of the standard dose. Any adjustment of the initial dose should be guided by toxicity (monitoring of blood counts) and effectiveness. If the dose is decreased: advise patients to seek medical attention when symptoms of myelosuppression (such as severe sore throat in combination with fever, regular nosebleeds and tendency to bruising) occur.</p>\n"}
  ]
)

print(completion.choices[0].message.content)
"""