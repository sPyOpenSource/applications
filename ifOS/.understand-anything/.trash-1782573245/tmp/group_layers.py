import json
import os

nodes_file = '/Users/xuyi/Source/OS/armOS/lib/jcore/test/ifOS/.understand-anything/tmp/arch-nodes.json'
output_file = '/Users/xuyi/Source/OS/armOS/lib/jcore/test/ifOS/.understand-anything/intermediate/layers.json'

with open(nodes_file, 'r') as f:
    nodes = json.load(f)

layers = {
    'core-kernel': {'name': 'Core Kernel', 'description': 'Microkernel core components', 'nodeIds': []},
    'device-drivers': {'name': 'Device Drivers', 'description': 'Device drivers and hardware abstraction', 'nodeIds': []},
    'filesystem': {'name': 'Filesystem', 'description': 'Filesystem implementation and buffer cache', 'nodeIds': []},
    'networking': {'name': 'Networking', 'description': 'Networking stack and RPC services', 'nodeIds': []},
    'standard-library': {'name': 'Standard Library', 'description': 'Standard Java API stubs and interfaces', 'nodeIds': []},
    'alternatives': {'name': 'Alternative Implementations', 'description': 'Alternative JDK implementations', 'nodeIds': []},
    'build-system': {'name': 'Build System', 'description': 'Project configuration and build scripts', 'nodeIds': []},
    'documentation': {'name': 'Documentation', 'description': 'Project documentation and changelogs', 'nodeIds': []},
    'others': {'name': 'Others', 'description': 'Miscellaneous files', 'nodeIds': []},
}

for node in nodes:
    node_id = node['id']
    # Use filePath if available, otherwise derive it from node_id
    file_path = node.get('filePath')
    if not file_path:
        if node_id.startswith('file:'):
            file_path = node_id[len('file:'):]
        elif node_id.startswith('config:'):
            file_path = node_id[len('config:'):]
        elif node_id.startswith('document:'):
            file_path = node_id[len('document:'):]
        else:
            file_path = node_id

    if file_path.startswith('src/jx/zero/'):
        layers['core-kernel']['nodeIds'].append(node_id)
    elif file_path.startswith('src/jx/devices/'):
        layers['device-drivers']['nodeIds'].append(node_id)
    elif file_path.startswith('src/jx/fs/'):
        layers['filesystem']['nodeIds'].append(node_id)
    elif file_path.startswith('src/jx/net/'):
        layers['networking']['nodeIds'].append(node_id)
    elif file_path.startswith('src/java/') or file_path.startswith('src/gnu/java/'):
        layers['standard-library']['nodeIds'].append(node_id)
    elif file_path.startswith('alternative/'):
        layers['alternatives']['nodeIds'].append(node_id)
    elif file_path.startswith('nbproject/') or file_path in ['build.xml', 'manifest.mf', 'META']:
        layers['build-system']['nodeIds'].append(node_id)
    elif node_id.startswith('document:') or (node_id.startswith('file:') and (file_path.endswith('README.TXT') or file_path == 'CHANGELOG' or file_path == 'README.rst')):
        layers['documentation']['nodeIds'].append(node_id)
    else:
        layers['others']['nodeIds'].append(node_id)

result = []
for layer_id, info in layers.items():
    if info['nodeIds']:
        result.append({
            'id': f'layer:{layer_id}',
            'name': info['name'],
            'description': info['description'],
            'nodeIds': info['nodeIds']
        })

os.makedirs(os.path.dirname(output_file), exist_ok=True)
with open(output_file, 'w') as f:
    json.dump(result, f, indent=2)

print(f"Successfully wrote {len(result)} layers to {output_file}")
