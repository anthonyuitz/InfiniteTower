# InfiniteTower

Infinite Tower is a game created in CS4730 as the final culuminating project with a team of four members. The general idea was a never-ending procedurally generated dungeon crawler with enemy monsters, bosses, items, stats, and a unique mechanic called reincarnation. Before challenging a boss, the player can choose to either restart the game from the start, keeping a percentage of their growth potential (the amount their stats grow when the character levels up). Death is permanent. 

Items include potions with a variety of effects like temporarily increasing stats, a shield, health restoration, and more. 

The possible stats are Str, Agi, and Luk as well as the respective growth potentials for each of these stats. On level up, the player can choose to allocate either 5 points to any stat or .2 points to any growth potential. Str increases damage, Agi increase dodge chance, Luk increases item drop chance from killing monsters.

Levels are procedurally generated through the following algorithm
1. A large number of rooms are created with random x, y coordinates and gaussian distributed width and height, mean and stddev dependent on the current level as levels grow larger as the player goes higher in the tower
2. A room is randomly selected from the list of generated rooms and all rooms that overlap with this room are removed from the list.
3. Step 2 is repeated until there are no rooms in the list
4. The rooms are connected in the order that they were picked, the first room is the "spawn" room and the last room is where the stairs are located
5. Each room is given a small percent chance to connect to other rooms in the list
6. The entire level and each room is surrounded by walls, with pathways connecting the rooms to each other becoming doorways instead of walls
7. Rooms are randomly filled with potions, enemies, and treasure chests
 
The code base in this repository only includes the code pertaining to Infinite Tower. The game engine that it was built on was not included in this public repository to prevent cheating for future students of CS4730 at the request of the professor. The engine was built individually by each team member and then combined before the creation of Infinite Dungeon, with the framework of the engine provided by the professor. 
