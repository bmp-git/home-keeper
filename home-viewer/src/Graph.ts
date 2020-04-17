import G6 from '@antv/g6';

export function initGraph(container: string, width: number, height: number) {
    G6.registerNode('dom-node', {
        draw: function (cfg: any, group: any) {
      
            const rect = group.addShape('dom', {
                attrs: {
                  width: cfg.size[0],
                  height: cfg.size[1],
                  html: `
                  <div style="background-color: #fff; border: 2px solid #5B8FF9; border-radius: 5px; width: ${cfg.size[0]-5}px; height: ${cfg.size[1]-5}px; display: flex;">
                    <div style="height: 100%; width: 33%; background-color: #CDDDFD">
                      <img alt="" style="line-height: 100%; padding-top: 6px; padding-left: 8px;" src=${cfg.logoIcon.img} width="20" height="20" />  
                    </div>
                    <div style="height: 100%; width: 67%;">
                        <span style="margin:auto; padding:auto; color: #5B8FF9">${cfg.label}</span>
                        <p>asdasdasd</p>
                        <p>asdasd</p>
                    </div>
                  </div>
                    `
                },
                name: 'dom-shape',
                //draggable: true
            });
            group.addShape('polygon', {
                attrs: {
                  points: [
                    [0, 0],
                    [cfg.size[0]*0.33, 0],
                    [cfg.size[0]*0.33, cfg.size[1]],
                    [0, cfg.size[1]],
                  ],
                  fill: 'white',
                  opacity: 0,
                },
                name: 'polygon-shape',
                draggable: true
              });      
          return rect
        }
    }, 'single-node');
    
    return new G6.Graph({
        container: container,
        width,
        height,
        renderer: 'svg',
        layout: {
          type: 'grid'
        },
        defaultNode : {
            type : 'dom-node',
            size : [270, 80]
        },
        defaultEdge : {
            size: 4
        },
        modes: {
          default: ['drag-node'],
        },
        nodeStateStyles: {
          hover: {
            lineWidth: 2,
            stroke: '#1890ff',
            fill: '#e6f7ff',
          },
        },
      });
}