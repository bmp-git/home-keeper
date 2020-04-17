class HomeNode {
    id: string;
    label: string;
    description: string;
    nodeType: string;
    size? : number[];
    logoIcon: any;
    descriptionCfg: any;
    anchorPoints: any;

    constructor( id: string, label: string, properties: any, nodeType: string) {
        this.id = id;
        this.label = label;
        this.description = JSON.stringify(properties);
        this.nodeType = nodeType;
        this.size =  [270, (80 + properties.length * 20)];
        let image = ""
        switch(nodeType) {
            case "room" : image = "https://image.flaticon.com/icons/svg/489/489870.svg" ; break;
            case "window" : image = "https://image.flaticon.com/icons/svg/544/544081.svg" ; break;
            case "door" : image = "https://image.flaticon.com/icons/svg/515/515094.svg" ; break;
        }
        this.descriptionCfg = {
            positions: 'bottom',
            style: {
              fill: '#f00'
            },
            paddingTop:0
        }
        this.anchorPoints =  [
            [0, 0.5],
            [1, 0.5],
            [0.5, 0],
            [0.5, 1]
          ]
        this.logoIcon = {
            show: true,
            x: 0,
            y: 0,
            img: image,
            width: 24,
            height: 24,
            offset: -10,
        }
    }
}

class HomeEdge {
    target: string;
    source: string;

    constructor( target: string, source: string) {
       this.target = target;
       this.source = source;
    }
}

export class HomeGraph {
    nodes: HomeNode[]
    edges: HomeEdge[]

    constructor( nodes: HomeNode[], edges: HomeEdge[]) {
        this.nodes = nodes;
        this.edges = edges;
     }
}

export function generateData(home: any) {
    const nodes: any[] = []
    const edges: any[] = []

    const floor = home.floors[0];
    floor.rooms.forEach((room: any) => {
        nodes.push(new HomeNode(room.name, room.name, room.properties, "room"));
        room.doors.forEach((door: any) => {
            if(!nodes.some(node => node.id === door.name)) {
                nodes.push(new HomeNode(door.name, door.name, door.properties, "door"));
                edges.push(new HomeEdge(door.name, door.rooms[0]));
                edges.push(new HomeEdge(door.rooms[1], door.name));
            }
        });

        room.windows.forEach((window: any) => {
            if(!nodes.some(node => node.id === window.name)) {
                nodes.push(new HomeNode(window.name, window.name, window.properties, "window"));
                edges.push(new HomeEdge(window.name, window.rooms[0]));
                edges.push(new HomeEdge(window.rooms[1], window.name));
            }
        
        });
    });

    return new HomeGraph(nodes, edges)
}