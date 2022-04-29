//
//  HomePageController.swift
//  AppShowcase
//
//  Created by Gagandeep on 31/08/21.
//

import UIKit
import RealmSwift

class HomePageController: BaseViewController {
    
    @IBOutlet weak var collectionView: UICollectionView!
    @IBOutlet weak var lowStockCollectionView: UICollectionView! {
        didSet {
            self.lowStockCollectionView.makeRoundCornerWithoutBorder(withRadius: 5)
            self.lowStockCollectionView.setShadow()
        }
    }
    @IBOutlet weak var searchBar: UISearchBar! {
        didSet {
            self.searchBar.delegate = self
            self.searchBar.placeholder = "Search"
        }
    }
    @IBOutlet weak var totalCountLbl: UILabel! {
        didSet {
            self.totalCountLbl.makeRoundCornerWithoutBorder(withRadius: 5)
        }
    }
    @IBOutlet var viewModel: HomePageViewModel!

    // MARK: - View Controller Life cycle

    override func viewDidLoad() {
        super.viewDidLoad()
        self.setLogoOnNavBarLeftItem()
    }
    
    override func viewWillAppear(_ animated: Bool) {
        super.viewWillAppear(animated)
        self.tabBarController?.tabBar.isHidden = false
        self.observeRealmChanges()
        loadProducts()
    }
    
    private func observeRealmChanges()  {
        // Observe collection notifications. Keep a strong
         // reference to the notification token or the
         // observation will stop.
        guard let collectionVw = self.collectionView else { return }
        guard let lowStockCollectionVw = self.lowStockCollectionView else { return }

        viewModel.notificationToken?.invalidate()
        viewModel.lowStockNotfnToken?.invalidate()
        
        viewModel.notificationToken = viewModel.filteredArray?.observe { [weak self] (changes: RealmCollectionChange) in
            self?.reactToRealmChanges(onCollection: collectionVw, realmChanges: changes)
         }
        viewModel.lowStockNotfnToken = viewModel.lowStockProducts?.observe { [weak self] (changes: RealmCollectionChange) in
            self?.reactToRealmChanges(onCollection: lowStockCollectionVw, realmChanges: changes)
            self?.lowStockCollectionView.isHidden = self?.viewModel.lowStockProducts?.count == 0
         }
    }
    
    func reactToRealmChanges(onCollection collectionView: UICollectionView,realmChanges changes: RealmCollectionChange<Results<StoreInventory>> )  {
        DispatchQueue.main.async {
            switch changes {
            case .initial:
                // Results are now populated and can be accessed without blocking the UI
                collectionView.reloadData()
            case .update(_, let deletions, let insertions, let modifications):
                // Query results have changed, so apply them to the UITableView
                collectionView.performBatchUpdates({
                    // Always apply updates in the following order: deletions, insertions, then modifications.
                    // Handling insertions before deletions may result in unexpected behavior.
                    collectionView.deleteItems(at: deletions.map({ IndexPath(row: $0, section: 0)}))
                    collectionView.insertItems(at:  insertions.map({ IndexPath(row: $0, section: 0) }))
                    collectionView.reloadItems(at: modifications.map({ IndexPath(row: $0, section: 0) }))
                }, completion: { finished in
                    // ...
                })
            case .error(let error):
                // An error occurred while opening the Realm file on the background worker thread
                fatalError("\(error)")
            }
        }

    }
    // MARK: - Private methods
    
    private func loadProducts() {
        self.viewModel.getProducts()
        self.collectionView.reloadData()
        if self.viewModel.lowStockProducts == nil || self.viewModel.lowStockProducts?.count == 0 {
            self.lowStockCollectionView.isHidden = true
        } else {
            self.lowStockCollectionView.isHidden = false
            self.lowStockCollectionView.reloadData()
        }
        self.totalCountLbl.text = "  Total Items: \(viewModel.filteredArray?.count ?? 0)  "
        self.setStoreInfoOnNavBarRight()
    }
    
    // MARK: - Button Actions
    
    @IBAction private func openSearchbar(_ sender: UIBarButtonItem) {
        searchBar.placeholder = "Search"
    }

    private func cancelBarButtonItemClicked() {
        self.searchBarCancelButtonClicked(self.searchBar)
    }
    
    @objc private func searchBarOpenAction(){
        searchBar.placeholder = "Search"
    }
}

// MARK:- Collection view Delegate and Data source methods

extension HomePageController: UICollectionViewDelegate, UICollectionViewDataSource, UICollectionViewDelegateFlowLayout {
    func numberOfSections(in collectionView: UICollectionView) -> Int {
        return 1
    }
    func collectionView(_ collectionView: UICollectionView, numberOfItemsInSection section: Int) -> Int {
        return collectionView ==  lowStockCollectionView ? self.viewModel.lowStockProducts?.count ?? 0 : self.viewModel.filteredArray?.count ?? 0
    }
    func collectionView(_ collectionView: UICollectionView, cellForItemAt indexPath: IndexPath) -> UICollectionViewCell {
        if collectionView ==  lowStockCollectionView {
            let cell = collectionView.dequeueReusableCell(withReuseIdentifier: CellIdentifier.lowStockCell, for: indexPath) as? ProductsCollectionViewCell
            cell?.productImage.makeRoundCornerWithoutBorder(withRadius: 5.0)
            let inventory = self.viewModel.lowStockProducts?[indexPath.row]
            cell?.model = inventory
            cell?.productQty.text = "Stock: Only \(inventory?.quantity ?? 0) Remaining"
            return cell!
        } else {
            let cell = collectionView.dequeueReusableCell(withReuseIdentifier: CellIdentifier.productCollectionViewCell, for: indexPath) as? ProductsCollectionViewCell
            cell?.makeRoundCornerWithoutBorder(withRadius: 15)
            cell?.setShadow()
            cell?.productImage.makeRoundCornerWithoutBorder(withRadius: 5.0)
            cell?.model = self.viewModel.cellRowIndexPath(index: indexPath.row)
            return cell!
        }
    }
    func collectionView(_ collectionView: UICollectionView, didSelectItemAt indexPath: IndexPath) {
        if collectionView ==  lowStockCollectionView {
            let result = self.viewModel.lowStockProducts?[indexPath.row]
            let vc = self.storyboard?.instantiateViewController(identifier: StoryboardID.productDetailPage.rawValue) as? ProductDetailController
            vc?.viewModel.inventory = result
            vc?.viewModel.product = RealmManager.shared.getProduct(withId: (result?.productId)!)
            self.navigationController?.pushViewController(vc!, animated: true)
        } else {
            let result = self.viewModel.cellRowIndexPath(index: indexPath.row)
            let vc = self.storyboard?.instantiateViewController(identifier: StoryboardID.productDetailPage.rawValue) as? ProductDetailController
            vc?.viewModel.inventory = result
            vc?.viewModel.product = RealmManager.shared.getProduct(withId: (result?.productId)!)
            self.navigationController?.pushViewController(vc!, animated: true)
        }
    }

       func collectionView(_ collectionView: UICollectionView, layout collectionViewLayout: UICollectionViewLayout, sizeForItemAt indexPath: IndexPath) -> CGSize {
           if collectionView ==  lowStockCollectionView {
               return CGSize(width: UIScreen.main.bounds.width -  16.0 * 2.0, height: 70)
           } else {
               let collectionViewWidth = (UIScreen.main.bounds.width - 16.0 * 3.0) / 2.0
               return CGSize(width: collectionViewWidth, height: 180)
           }
       }
       
}

// MARK: - SearchBar Delegates
extension HomePageController:UISearchBarDelegate{
    func searchBar(_ searchBar: UISearchBar, textDidChange searchText: String){
        self.viewModel.searchList(text: searchText)
        self.collectionView.reloadData()
    }
    @objc func searchBarCancelButtonClicked(_ searchBar: UISearchBar) {
        self.loadProducts()
    }
}
