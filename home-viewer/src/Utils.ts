
export function flatHome(home: any) {
    return home.floors.flatMap((f : any) => {
        return [{entity: f, type: "floor", floor: f.name, level: f.level, url: `/home/floors/${f.name}`}].concat(f.rooms.flatMap((r: any) => {
            const room = { entity: r, type: "room", floor: f.name, room: r.name, level: f.level, url: `/home/floors/${f.name}/rooms/${r.name}` };
            const doors = r.doors.map((d: any) => (
                { entity: d, type: "door", floor: f.name, room: r.name, level: f.level, url: `/home/floors/${f.name}/rooms/${r.name}/doors/${d.name}` }
            ));
            const windows = r.windows.map((w: any) => (
                { entity: w, type: "window", floor: f.name, room: r.name, level: f.level, url: `/home/floors/${f.name}/rooms/${r.name}/windows/${w.name}` }
            ));
            return [room].concat(doors, windows);
        }));
    })
}