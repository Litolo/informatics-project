import json

f = open('../settings.json')
_config = json.load(f)
f.close()
def get(key):
    return _config.get(key)