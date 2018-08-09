# Cultural Heritage Entity Linker

CHEL is a Named Entity Disambiguation tool which was developed specifically to
tackle problems with Entity Linking in Cultural Heritage. The research project
which was the genesis of this tool involved entity analysis on 17th century
Irish depositions collected during the 1641 Rebellion in Ireland.

From these manuscripts, two significant problems with off-the-shelf entity
linking tools were identified:

1. The noisy nature of spelling in archaic manuscripts makes the identification
   of candidate referents difficult when searching the knowledge base
2. Due to the specialised nature of the depositions no single source of
   knowledge can adequately cover the breadth of entities they contain.

CHEL incorporates a variety of methods for fuzzy string matching on surface
forms which can be controlled from the `chel.properties` file. Most
significantly, CHEL facilitates entity linking with respect to multiple
different knowledge bases simultaneously. The development of CHEL was heavily
inspired by two other significant research projects:

1. AGDISTIS: A graph based Entity Linking tool which uses HITS to evaluate
   networks of candidate referents. In an exploratory study, AGDISTIS was found
   to be the most reliable tool for performing entity linking on the Irish
   depositions.
2. REDEN: An entity linking tool with similar goals to CHEL. REDEN is capable
   of performing entity linking across multiple knowledge bases and uses
   degree centrality measures to evaluate the quality of candidates as
   referents.

CHEL is built on top of the FREME project and can be deployed as an e-service
in a FREME instance.