#! /bin/sh

rm -rf scenario2/*
rm -rf scenario2/.git
cd scenario2
git init
git config user.email "pankajtandon@gmail.com"
git checkout -b develop
sleep 1
touch initial.txt
echo "Initial text" >> initial.txt
git add .
git commit -am "Initial commit"
sleep 1
git checkout -b master
sleep 1
git checkout -b feature/STW-115-dream-more
touch dream.txt
git add .
echo "dream about blue waters" >> dream.txt
git commit -am "Dreamt about blue waters"
sleep 1
echo "dreamt about daisies!" >> dream.txt
git commit -am "Dreamt about daisies"
sleep 1
echo "dreamt about world peace" >> dream.txt
git commit -am "Dreamt about world peace"
sleep 1
echo "Done dreaming" >> dream.txt
git commit -am "STW-115 Done dreaming!"
sleep 1
git checkout `git log --all --oneline --graph --decorate | grep "Dreamt about blue" | cut -c3-9`
sleep 1
git checkout -b feature/STW-116-eat-less
sleep 1
touch eat.txt
echo "Ate 2000 calories" >> eat.txt
git add .
sleep 1
git commit -am "Ate 2000 calories"
sleep 2
echo "Ate 1000 calories" >> eat.txt
git commit -am "Ate 1000 calories"
sleep 1
echo "Ate 100 calories" >> eat.txt
git commit -am "Ate 100 calories"
sleep 1
echo "Ate nothing!" >> eat.txt
git commit -am "Ate nothing!"
sleep 1
echo "Done eating!" >> eat.txt
git commit -am "STW-116 Done eating!"
sleep 1
git checkout `git log --all --oneline --graph --decorate | grep "Dreamt about blue" | cut -c3-9`
sleep 1
git checkout -b feature/STW-117-worry-less
sleep 1
touch worry.txt
sleep 1
git add .
sleep 1
echo "Worry about my hair" >> worry.txt
git commit -am "Worried about my hair"
sleep 1
echo "Worried about my body" >> worry.txt
git commit -am "Worried about my body"
sleep 1
echo "Done worrying" >>  worry.txt
git commit -am "STW-117 Done worrying"
sleep 1
git checkout develop
sleep 1
git merge --no-ff feature/STW-115-dream-more --no-edit
sleep 1
git checkout -b "feature/STW-118-plan-less"
touch plan.txt
git add .
echo "Planned for next year" >> plan.txt
git commit -am "Planned for next year"
sleep 1
echo "Planned for next month" >> plan.txt
git commit -am "Planned for Jan"
sleep 1
echo "Planned for tomorrow" >> plan.txt
git commit -am "Planned for tomorrow"
sleep 1
echo "Done planning" >> plan.txt
git commit -am "STW-118 Done planning"
git rebase develop 
sleep 1
git checkout `git log --all --oneline --graph --decorate | grep "Merge branch \'feature/STW-115-dream-more\' into develop" | cut -c3-9`
sleep 1
git checkout develop
sleep 1
git merge --no-ff feature/STW-118-plan-less --no-edit
sleep 1
git checkout feature/STW-116-eat-less
sleep 1
git rebase develop
sleep 1
git checkout develop
sleep 1
git merge --no-ff feature/STW-116-eat-less --no-edit 
sleep 1
git checkout feature/STW-117-worry-less
sleep 1
git rebase develop
sleep 1
git checkout develop
sleep 1
git merge --no-ff feature/STW-117-worry-less --no-edit
git checkout master
sleep 1
git merge --no-ff develop --no-edit
git tag R_1.0.0
git checkout -b feature/STW-101-plant-some-trees
sleep 1
touch trees.txt
echo "Planted a mango tree" > trees.txt
git add .
git commit -am "Planted a mango tree"
sleep 1
echo "Planted a fig tree" >> trees.txt
git commit -am "Planted a fig tree"
sleep 1
echo "Done planting" >> trees.txt
git commit -am "STW-101 Done planting trees"
sleep 1
git checkout develop
sleep 1
git checkout -b feature/STW-109-yoga
sleep 1
touch yoga.txt
echo "Do surya namaskar" >> yoga.txt
git add .
sleep 1
git commit -am "Added suryanamskar"
sleep 1
echo "Stand on head" >> yoga.txt
git commit -am "Standing on head"
sleep 1
echo "Done with Yoga" >> yoga.txt
git commit -am "STW-109 Done with Yoga"
sleep 1
git checkout develop
sleep 1
git merge --no-ff feature/STW-101-plant-some-trees --no-edit
sleep 1
git checkout develop
sleep 1
git merge --no-ff feature/STW-109-yoga --no-edit
sleep 1
git checkout master
sleep 1
git merge --no-ff develop --no-edit
git tag R_1.0.1

